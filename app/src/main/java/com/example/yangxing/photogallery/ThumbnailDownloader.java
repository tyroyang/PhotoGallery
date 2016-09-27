package com.example.yangxing.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**负责后台下载图片的线程
 * Created by yangxing on 2016/9/7.
 */
public class ThumbnailDownloader<T> extends HandlerThread{
    private static final String TAG="ThumnailDownloaer";
    private Boolean mHasQuit=false;
    private static final int MESSAGE_DOWNLOAD=0;
    private Handler mRequestHandler;
    private ConcurrentMap<T,String> mRequestMap=new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target,Bitmap thumbnail);
    }
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }



    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler=responseHandler;

    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==MESSAGE_DOWNLOAD){
                    T target= (T) msg.obj;
                    Log.i(TAG, "handleMessage: "+mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }



    @Override
    public boolean quit() {
        mHasQuit=true;
        return super.quit();
    }
    //T  target 具体标志哪次下载   url 下载地址
    public void queueThumnail(T target,String url){
        Log.i(TAG, "Got a url: "+url);
        if(url==null){
            mRequestMap.remove(target);
        }
        else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target)
                    .sendToTarget();
        }
    }
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
    private void handleRequest(final T target) {
        try {
            final String url=mRequestMap.get(target);
            if (url==null){
                return;
            }
            byte[] bitmapBytes=new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap= BitmapFactory
                    .decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            Log.i(TAG, "bitmap创建成功 ");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target)!=url||mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "图片下载失败",e );
        }


    }
}
