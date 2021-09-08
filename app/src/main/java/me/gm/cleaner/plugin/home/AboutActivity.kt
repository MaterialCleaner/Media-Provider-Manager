package me.gm.cleaner.plugin.home

import android.widget.ImageView
import android.widget.TextView
import com.drakeet.about.*
import me.gm.cleaner.plugin.BuildConfig
import me.gm.cleaner.plugin.R

// https://developer.android.com/training/data-storage/shared/media?hl=zh-cn
// see also {@link me.gm.cleaner.plugin.test.TestAdapter}

class AboutActivity : ThemedAboutActivity() {
    override fun onCreateHeader(icon: ImageView, slogan: TextView, version: TextView) {
        icon.setImageDrawable(packageManager.defaultActivityIcon)
        slogan.text = getString(R.string.app_name)
        version.text = BuildConfig.VERSION_NAME
    }

    override fun onItemsCreated(items: MutableList<Any>) {
        items.add(Category("介绍与帮助"))
        items.add(Card("这是一个来自 drakeet 独立开发的 纯纯打码 App 中的关于页面。\n\n纯纯打码 是一款专注打码的轻应用，包含功能：传统马赛克、毛玻璃效果、选区和手指模式打，更有创新型高亮打码和 LowPoly 风格马赛克。\n只为满足一个纯纯的打码需求，让打码也能成为一种赏心悦目。\n\n如今我将这个页面从这个精致的应用中抽离出来开源，和我一贯做的开源项目一样，仅仅是为了让更多人获得方便。它代码干净易读，布局清晰优雅，在尺寸方面花费了许多调整的时间。\n\n很高兴能够分享给大家！最后，为了展示在卡片中能够自动识别链接和支持链接，我粘贴了纯纯打码的下载地址，你可以尝试点击试试，谢谢：\n\nhttp://www.coolapk.com/apk/me.drakeet.puremosaic\n\nhttps://fir.im/puremosaic"))

        items.add(Category("Developers"))
        items.add(
            Contributor(
                R.drawable.ic_outline_apps_24, "drakeet", "Developer & designer",
                "http://weibo.com/drak11t"
            )
        )
        items.add(
            Contributor(
                R.drawable.ic_outline_apps_24, "黑猫酱", "Developer", "https://drakeet.me"
            )
        )
        items.add(Contributor(R.drawable.ic_outline_apps_24, "小艾大人", "Developer"))

        items.add(Category("我独立开发的应用"))
        items.add(
            Recommendation(
                0, "纯纯写作",
                "https://storage.recommend.wetolink.com/storage/app_recommend/images/YBMHN6SRpZeF0VHbPZWZGWJ2GyB6uaPx.png",
                "com.drakeet.purewriter",
                "快速的纯文本编辑器，我们希望写作能够回到原本的样子：纯粹、有安全感、随时、绝对不丢失内容、具备良好的写作体验。",
                "https://www.coolapk.com/apk/com.drakeet.purewriter",
                "2017-10-09 16:46:57",
                "2017-10-09 16:46:57", 2.93, true
            )
        )
        items.add(
            Recommendation(
                1, "纯纯打码",
                "http://image.coolapk.com/apk_logo/2016/0831/ic_pure_mosaic-2-for-16599-o_1argff2ddgvt1lfv1b3mk2vd6pq-uid-435200.png",
                "me.drakeet.puremosaic",
                "专注打码的轻应用，包含功能：传统马赛克、毛玻璃效果、选区和手指模式打码，更有创新型高亮打码和 LowPoly 风格马赛克。只为满足一个纯纯的打码需求，让打码也能成为一种赏心悦目。",
                "https://www.coolapk.com/apk/me.drakeet.puremosaic",
                "2017-10-09 16:46:57",
                "2017-10-09 16:46:57", 2.64, true
            )
        )

        items.add(Category("Open Source Licenses"))
        items.add(
            License(
                "MultiType", "drakeet", License.APACHE_2, "https://github.com/drakeet/MultiType"
            )
        )
        items.add(
            License(
                "about-page", "drakeet", License.APACHE_2, "https://github.com/drakeet/about-page"
            )
        )
    }
}
