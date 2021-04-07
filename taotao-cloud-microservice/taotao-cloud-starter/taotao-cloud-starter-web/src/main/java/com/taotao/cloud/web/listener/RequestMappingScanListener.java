package com.taotao.cloud.web.listener;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import com.taotao.cloud.common.constant.CommonConstant;
import com.taotao.cloud.common.utils.LogUtil;
import com.taotao.cloud.redis.repository.RedisRepository;
import io.swagger.v3.oas.annotations.Operation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * 请求资源扫描监听器
 *
 * @author pangu
 */
public class RequestMappingScanListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final AntPathMatcher PATH_MATCH = new AntPathMatcher();
	private final Set<String> ignoreApi = new HashSet<>();
	private final RedisRepository redisRepository;

	public RequestMappingScanListener(RedisRepository redisRepository) {
		this.redisRepository = redisRepository;
		this.ignoreApi.add("/error");
		this.ignoreApi.add("/swagger-resources/**");
		this.ignoreApi.add("/v2/api-docs-ext/**");
	}

	/**
	 * 默认事件
	 *
	 * @param event ApplicationReadyEvent
	 */
	@Override
	public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
		try {
			ConfigurableApplicationContext applicationContext = event.getApplicationContext();
			Environment env = applicationContext.getEnvironment();
			// 获取微服务模块名称
			String microService = env.getProperty("spring.application.name", "application");
			if (redisRepository == null || applicationContext
				.containsBean("resourceServerConfiguration")) {
				LogUtil.warn("[{}]忽略接口资源扫描", microService);
				return;
			}

			// 所有接口映射
			RequestMappingHandlerMapping mapping = applicationContext
				.getBean(RequestMappingHandlerMapping.class);
			// 获取url与类和方法的对应信息
			Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
			List<Map<String, String>> list = new ArrayList<>();
			for (Map.Entry<RequestMappingInfo, HandlerMethod> m : map.entrySet()) {
				RequestMappingInfo info = m.getKey();
				HandlerMethod method = m.getValue();

				Operation methodAnnotation = method.getMethodAnnotation(Operation.class);
				if (methodAnnotation != null) {
					if (methodAnnotation.hidden()) {
						// 忽略的接口不扫描
						continue;
					}
				}

				// 请求路径
				PatternsRequestCondition p = info.getPatternsCondition();
				String urls = "";
				if (Objects.nonNull(p)) {
					urls = getUrls(p.getPatterns());
					if (isIgnore(urls)) {
						continue;
					}
				}

				Set<MediaType> mediaTypeSet = info.getProducesCondition().getProducibleMediaTypes();
				for (MethodParameter params : method.getMethodParameters()) {
					if (params.hasParameterAnnotation(RequestBody.class)) {
						mediaTypeSet.add(MediaType.APPLICATION_JSON_UTF8);
						break;
					}
				}

				String mediaTypes = getMediaTypes(mediaTypeSet);
				// 请求类型
				RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();
				String methods = getMethods(methodsCondition.getMethods());
				Map<String, String> api = Maps.newHashMap();
				// 类名
				String className = method.getMethod().getDeclaringClass().getName();
				// 方法名
				String methodName = method.getMethod().getName();
				String fullName = className + "." + methodName;
				// md5码
				String md5 = DigestUtils.md5DigestAsHex((microService + urls).getBytes());
				String summary = "";
				String description = "";
				String auth = "0";

				if (methodAnnotation != null) {
					summary = methodAnnotation.summary();
					description = methodAnnotation.description();
				}

				// 判断是否需要权限校验
//				PreAuth preAuth = method.getMethodAnnotation(PreAuth.class);
//				if (preAuth != null) {
//					auth = "1";
//				}

				summary = StrUtil.isBlank(summary) ? methodName : summary;
				api.put("summary", summary);
				api.put("description", description);
				api.put("path", urls);
				api.put("code", md5);
				api.put("className", className);
				api.put("methodName", methodName);
				api.put("method", methods);
				api.put("serviceId", microService);
				api.put("contentType", mediaTypes);
				api.put("auth", auth);
				list.add(api);
			}

			// 放入redis缓存
			Map<String, Object> res = Maps.newHashMap();
			res.put("serviceId", microService);
			res.put("size", list.size());
			res.put("list", list);

			redisRepository.setExpire(
				CommonConstant.TAOTAO_CLOUD_API_RESOURCE,
				res,
				CommonConstant.TAOTAO_CLOUD_RESOURCE_EXPIRE);
			redisRepository.setExpire(
				CommonConstant.TAOTAO_CLOUD_SERVICE_RESOURCE,
				microService,
				CommonConstant.TAOTAO_CLOUD_RESOURCE_EXPIRE);

			LogUtil.info("资源扫描结果:serviceId=[{}] size=[{}] redis缓存key=[{}]",
				microService,
				list.size(),
				CommonConstant.TAOTAO_CLOUD_API_RESOURCE);
		} catch (Exception e) {
			LogUtil.error("error: {}", e.getMessage());
		}
	}

	private String getUrls(Set<String> urls) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String url : urls) {
			stringBuilder.append(url).append(",");
		}
		if (urls.size() > 0) {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}
		return stringBuilder.toString();
	}

	/**
	 * 是否是忽略的Api
	 *
	 * @param requestPath 请求地址
	 * @return boolean
	 */
	private boolean isIgnore(String requestPath) {
		for (String path : ignoreApi) {
			if (PATH_MATCH.match(path, requestPath)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取媒体类型
	 *
	 * @param mediaTypes 类型SET集
	 * @return String
	 */
	private String getMediaTypes(Set<MediaType> mediaTypes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (MediaType mediaType : mediaTypes) {
			stringBuilder.append(mediaType.toString()).append(",");
		}
		if (mediaTypes.size() > 0) {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}
		return stringBuilder.toString();
	}

	/**
	 * 获取方法
	 *
	 * @param requestMethods 请求方法
	 * @return String
	 */
	private String getMethods(Set<RequestMethod> requestMethods) {
		StringBuilder stringBuilder = new StringBuilder();
		for (RequestMethod requestMethod : requestMethods) {
			stringBuilder.append(requestMethod.toString()).append(",");
		}
		if (requestMethods.size() > 0) {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}
		return stringBuilder.toString();
	}
}