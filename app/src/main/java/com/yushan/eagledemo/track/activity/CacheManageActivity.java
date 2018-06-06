package com.yushan.eagledemo.track.activity;

import android.os.Bundle;
import android.view.View;

import com.baidu.trace.api.track.ClearCacheTrackRequest;
import com.baidu.trace.api.track.ClearCacheTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.api.track.QueryCacheTrackRequest;
import com.baidu.trace.api.track.QueryCacheTrackResponse;
import com.yushan.eagledemo.R;
import com.yushan.eagledemo.track.TrackApplication;
import com.yushan.eagledemo.track.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 缓存管理
 */
public class CacheManageActivity extends BaseActivity {

    private TrackApplication trackApp = null;
    private OnTrackListener trackListener = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.cache_manage_title);
        setOptionsButtonInVisible();

        trackApp = (TrackApplication) getApplicationContext();
        trackListener = new OnTrackListener() {
            @Override
            public void onQueryCacheTrackCallback(QueryCacheTrackResponse response) {
                ToastUtils.showToastCentre(CacheManageActivity.this, response.toString());
            }

            @Override
            public void onClearCacheTrackCallback(ClearCacheTrackResponse response) {
                ToastUtils.showToastCentre(CacheManageActivity.this, response.toString());
            }
        };

    }

    /**
     * 查询缓存轨迹
     *
     * @param v
     */
    public void onQueryCacheTrack(View v) {
        QueryCacheTrackRequest request = new QueryCacheTrackRequest(trackApp.getTag(),
                trackApp.serviceId, trackApp.entityName);
        trackApp.mClient.queryCacheTrack(request, trackListener);
    }

    /**
     * 清除缓存轨迹
     *
     * @param v
     */
    public void onClearCacheTrack(View v) {
        List<String> entityNames = new ArrayList<>();
        entityNames.add(trackApp.entityName);
        ClearCacheTrackRequest request = new ClearCacheTrackRequest(trackApp.getTag(),
                trackApp.serviceId, entityNames);
        trackApp.mClient.clearCacheTrack(request, trackListener);
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_cache_manage;
    }

}
