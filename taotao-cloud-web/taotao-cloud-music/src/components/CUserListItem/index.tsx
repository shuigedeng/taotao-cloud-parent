import Taro, {FC} from '@tarojs/taro'
import {AtIcon} from 'taro-ui'
import {Image, View} from '@tarojs/components'

import './index.less'

type Props = {
  userInfo: {
    avatarUrl: string,
    nickname: string,
    signature?: string,
    gender: number,
    userId: number
  },
  clickFunc?: (number) => any
}
const CUserListItem: FC<Props> = ({userInfo, clickFunc}) => {
  function goDetail() {
    if (clickFunc) {
      clickFunc(userInfo.userId)
    }
  }

  if (!userInfo) {
    return null
  }

  return (
    <View className='userListItem_components' onClick={() => goDetail()}>
      <Image
        src={`${userInfo.avatarUrl}?imageView&thumbnail=250x0`}
        className='userListItem__avatar'
      />
      <View className='userListItem__info'>
        <View className='userListItem__info__name'>
          {userInfo.nickname}
          {
            userInfo.gender === 1 ? <AtIcon prefixClass='fa' value='mars' size='12' color='#5cb8e7'
                                            className='userListItem__info__icon'/> : ''
          }
          {
            userInfo.gender === 2 ? <AtIcon prefixClass='fa' value='venus' size='12' color='#f88fb8'
                                            className='userListItem__info__icon venus'/> : ''
          }
        </View>
        <View className='userListItem__info__signature'>{userInfo.signature || ''}</View>
      </View>
    </View>
  )
}

export default CUserListItem
