package com.devlomi.commune.utils.glide;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.nio.ByteBuffer;

import ezvcard.util.org.apache.commons.codec.binary.Base64;

public final class Base64ModelLoader implements ModelLoader<String, ByteBuffer> {

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(String model, int width, int height, Options options) {
        return new LoadData<>(new ObjectKey(model), new Base64DataFetcher(model));
    }

    @Override
    public boolean handles(String model) {
        return Base64.isBase64(model);
    }
}
