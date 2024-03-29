package com.xiaoyv404.mirai.service.bilibili

import com.xiaoyv404.mirai.databace.Bilibili
import com.xiaoyv404.mirai.service.accessControl.authorityIdentification
import com.xiaoyv404.mirai.service.getUserInformation
import com.xiaoyv404.mirai.service.tool.KtorUtils
import com.xiaoyv404.mirai.service.tool.parsingVideoDataString
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import java.io.InputStream

val format = Json { ignoreUnknownKeys = true }

@KtorExperimentalAPI
fun biliVideoEntrance() {
    GlobalEventChannel.subscribeGroupMessages {
        finding(Bilibili.biliBvFind){
            val bv = it.value
            if ((authorityIdentification(
                    sender.id,
                    group.id,
                    "BiliBiliParsing"
                )) && (getUserInformation(sender.id).bot != true)
            ) {
                uJsonVideo(
                    KtorUtils.normalClient.get(
                        "https://api.bilibili.com/x/web-interface/view?bvid=$bv"
                    ),group
                )
            }
        }
        finding(Bilibili.biliAvFind) {
            val av = it.groups[2]!!.value
            if ((authorityIdentification(
                    sender.id,
                    group.id,
                    "BiliBiliParsing"
                )) && (getUserInformation(sender.id).bot != true)
            ) {
                uJsonVideo(
                    KtorUtils.normalClient.get(
                        "https://api.bilibili.com/x/web-interface/view?aid=$av"
                    ),group
                )
            }
        }
    }
}

//用于格式化Json并发送
@KtorExperimentalAPI
suspend fun uJsonVideo(uJsonVideo: String, group: Contact) {
    /**
     * 如果pJson中含有data字段时不会抛出[SerializationException]，不含有则反之
     * 当未抛出[SerializationException]异常时，正常执行，使用[VideoDataJson]格式化并发送
     * 当抛出[SerializationException]异常时，被catch抓住并使用[AbnormalVideoDataJson]格式化并发送
     */
    try {
        val pJson = format.decodeFromString<VideoDataJson>(uJsonVideo)
        group.sendMessage(
            KtorUtils.normalClient.get<InputStream>(pJson.data.pic)
                .uploadAsImage(group, )
                .plus(parsingVideoDataString(pJson))
        )
    } catch (e: SerializationException) {
        val pJson = format.decodeFromString<AbnormalVideoDataJson>(uJsonVideo)
        when (pJson.code) {
            -404  -> group.sendMessage("喵, 视频不存在哦")
            -400  -> group.sendMessage("404又出Bug惹, 快去叫主人来修叭")
            -403  -> group.sendMessage("404的权限不足哦")
            62002 -> group.sendMessage("视频不可见惹, 这个死已婚又干了些什么啊")
        }
    }
}