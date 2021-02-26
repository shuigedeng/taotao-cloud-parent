/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taotao.cloud.elasticsearch.service.impl;

import com.taotao.cloud.core.model.PageResult;
import com.taotao.cloud.elasticsearch.builder.SearchBuilder;
import com.taotao.cloud.elasticsearch.model.SearchDto;
import com.taotao.cloud.elasticsearch.service.ISearchService;
import java.io.IOException;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * 搜索服务实现
 *
 * @author dengtao
 * @version 1.0.0
 * @since 2020/5/3 07:48
 */
public class SearchServiceImpl implements ISearchService {

	private final ElasticsearchRestTemplate elasticsearchRestTemplate;

	public SearchServiceImpl(ElasticsearchRestTemplate elasticsearchRestTemplate) {
		this.elasticsearchRestTemplate = elasticsearchRestTemplate;
	}

	@Override
	public PageResult<String> strQuery(String indexName, SearchDto searchDto) throws IOException {
		return SearchBuilder.builder(elasticsearchRestTemplate, indexName)
			.setStringQuery(searchDto.getQueryStr())
			.addSort(searchDto.getSortCol(), SortOrder.DESC)
			.setIsHighlight(searchDto.getIsHighlighter())
			.getPage(searchDto.getPage(), searchDto.getLimit());
	}
}