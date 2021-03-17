package com.droidninja.imageeditengine.filter;

import android.graphics.Bitmap;
import com.droidninja.imageeditengine.model.ImageFilter;
import com.droidninja.imageeditengine.utils.FilterHelper;
import java.util.ArrayList;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;

public final class GetFiltersObservable {
    public Observable<ArrayList<ImageFilter>> getObservable() {
        return new Observable<ArrayList<ImageFilter>>() {
            @Override
            protected void subscribeActual(Observer<? super ArrayList<ImageFilter>> observer) {

            }

        }.create(new ObservableOnSubscribe<ArrayList<ImageFilter>>() {
          @Override
          public void subscribe(ObservableEmitter<ArrayList<ImageFilter>> emitter) throws Exception {
            FilterHelper filterHelper = new FilterHelper();
            ArrayList<ImageFilter> filters = filterHelper.getFilters();
            for (int index = 0; index < filters.size(); index++) {
              ImageFilter imageFilter = filters.get(index);
              imageFilter.filterImage = PhotoProcessing.filterPhoto(getScaledBitmap(srcBitmap), imageFilter);
            }
            emitter.onNext(filters);
            emitter.onComplete();
          }
        });
    }

    private Bitmap srcBitmap;

    public GetFiltersObservable( Bitmap srcBitmap) {
        this.srcBitmap = srcBitmap;
    }

    private Bitmap getScaledBitmap(Bitmap srcBitmap) {
        // Determine how much to scale down the image
        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();

        int targetWidth = 320;
        int targetHeight = 240;
        if (srcWidth < targetWidth || srcHeight < targetHeight) {
            return srcBitmap;
        }

        float scaleFactor =
                Math.max(
                        (float) srcWidth / targetWidth,
                        (float) srcHeight / targetHeight);

        return
                Bitmap.createScaledBitmap(
                        srcBitmap,
                        (int) (srcWidth / scaleFactor),
                        (int) (srcHeight / scaleFactor),
                        true);
    }
}// end inner class