import Taro, {FC, useEffect, useState} from '@tarojs/taro'
import {ScrollView, View} from '@tarojs/components'
import CLoading from '../../../../components/CLoading'
import api from '../../../../services/api'
import CUserListItem from '../../../../components/CUserListItem'
import './index.less'

type userList = Array<{
  avatarUrl: string,
  nickname: string,
  signature?: string,
  gender: number,
  userId: number
}>


const Page: FC = () => {
  const [userId] = useState<number>(Taro.getStorageSync('userId'))
  const [userList, setUserList] = useState<userList>([])
  const [hasMore, setHasMore] = useState<boolean>(true)

  useEffect(() => {
    getFollowList()
  }, [])

  function goUserDetail() {
    Taro.showToast({
      title: '详情页面正在开发中，敬请期待',
      icon: 'none'
    })
    // Taro.navigateTo({
    //   url: `/pages/user/index?id=${id}`
    // })
  }

  function getFollowList() {
    console.log('start')
    if (!hasMore) {
      return
    }
    api.get('/user/follows', {
      uid: userId,
      limit: 20,
      offset: userList.length
    }).then((res) => {
      setUserList(userList.concat(res.data.follow))
      setHasMore(res.data.more)
    })
  }

  return (
    <View className='my_focus_container'>
      <ScrollView scrollY className='userList' onScrollToLower={getFollowList}>
        {
          userList.map((item) => <CUserListItem userInfo={item} key={item.userId} clickFunc={goUserDetail}/>)
        }
        <CLoading hide={!hasMore}/>
      </ScrollView>
    </View>
  )
}

Page.config = {
  navigationBarTitleText: '我的关注'
}

export default Page
