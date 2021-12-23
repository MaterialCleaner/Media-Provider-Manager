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

package me.gm.cleaner.plugin.mediastore;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;

import java.nio.ByteBuffer;

/**
 * An implementation of {@link ModelLoader} to support {@link com.bumptech.glide.Glide} loading for
 * media store {@link Uri}.
 * <p>
 * You should register this in {@link com.bumptech.glide.module.AppGlideModule}, see
 * <a href="https://bumptech.github.io/glide/tut/custom-modelloader.html#registering-our-modelloader-with-glide">
 * Registering our ModelLoader with Glide</a> for more information.
 * <p>
 * This is backed by {@link MediaMetadataRetriever}.
 */
public class MediaModelLoader implements ModelLoader<Uri, ByteBuffer> {
    private final Context mContext;

    MediaModelLoader(Context context) {
        mContext = context;
    }

    @Override
    public boolean handles(@NonNull Uri uri) {
        return true;
    }

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull Uri uri, int width, int height,
                                              @NonNull Options options) {
        return new LoadData<>(new ObjectKey(uri), new MediaMetadataFetcher(uri));
    }

    class MediaMetadataFetcher implements DataFetcher<ByteBuffer> {
        private final Uri mUri;

        private MediaMetadataFetcher(Uri uri) {
            mUri = uri;
        }

        @Override
        public void loadData(@NonNull Priority priority,
                             @NonNull DataCallback<? super ByteBuffer> callback) {
            try (var retriever = new MediaMetadataRetriever()) {
                retriever.setDataSource(mContext, mUri);
                var data = retriever.getEmbeddedPicture();
                ByteBuffer byteBuffer = ByteBuffer.wrap(data);
                callback.onDataReady(byteBuffer);
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void cancel() {
        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    public static class Factory implements ModelLoaderFactory<Uri, ByteBuffer> {
        @NonNull
        private final Context mContext;

        public Factory(@NonNull Context context) {
            mContext = context.getApplicationContext();
        }

        @NonNull
        @Override
        public ModelLoader<Uri, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new MediaModelLoader(mContext);
        }

        @Override
        public void teardown() {
        }
    }
}
