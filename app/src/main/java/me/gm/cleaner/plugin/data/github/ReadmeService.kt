/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.data.github

import android.content.Context
import me.gm.cleaner.plugin.ktx.hasWifiTransport
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import java.io.File

interface ReadmeService {
    @get:GET("README.md")
    val en: Call<String>

    @get:GET("README_zh-CN.md")
    val zh: Call<String>

    companion object {
        const val BASE_URL =
            "https://raw.githubusercontent.com/MaterialCleaner/Media-Provider-Manager/main/"

        fun create(context: Context): ReadmeService {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val originalResponse = chain.proceed(chain.request())
                    if (context.hasWifiTransport) {
                        val maxAge = 60 // read from cache for 1 minute
                        originalResponse.newBuilder()
                            .header("Cache-Control", "public, max-age=$maxAge")
                            .build()
                    } else {
                        val maxStale = 60 * 60 * 24 * 28 // tolerate 4-weeks stale
                        originalResponse.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                            .build()
                    }
                }
                .cache(Cache(File(context.cacheDir, "okhttp"), 1024L * 1024L))
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(ReadmeService::class.java)
        }
    }
}
