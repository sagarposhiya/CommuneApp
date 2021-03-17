package com.devlomi.commune.utils.glide;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.nio.ByteBuffer;

public class Base64DataFetcher implements DataFetcher<ByteBuffer> {

    private final String model;

    Base64DataFetcher(String model) {
        this.model = model;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super ByteBuffer> callback) {
        String base64Section = model;
        byte[] data = Base64.decode(base64Section, Base64.DEFAULT);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        callback.onDataReady(byteBuffer);
    }


    @Override
    public void cleanup() {
        // Intentionally empty only because we're not opening an InputStream or another I/O resource!
    }

    @Override
    public void cancel() {
        // Intentionally empty.
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
