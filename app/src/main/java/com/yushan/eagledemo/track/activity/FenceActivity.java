package com.yushan.eagledemo.track.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.api.fence.CircleFence;
import com.baidu.trace.api.fence.CreateFenceRequest;
import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceRequest;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceAlarmInfo;
import com.baidu.trace.api.fence.FenceInfo;
import com.baidu.trace.api.fence.FenceListRequest;
import com.baidu.trace.api.fence.FenceListResponse;
import com.baidu.trace.api.fence.FenceShape;
import com.baidu.trace.api.fence.FenceType;
import com.baidu.trace.api.fence.HistoryAlarmRequest;
import com.baidu.trace.api.fence.HistoryAlarmResponse;
import com.baidu.trace.api.fence.MonitoredStatusByLocationResponse;
import com.baidu.trace.api.fence.MonitoredStatusResponse;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.fence.PolygonFence;
import com.baidu.trace.api.fence.PolylineFence;
import com.baidu.trace.api.fence.UpdateFenceRequest;
import com.baidu.trace.api.fence.UpdateFenceResponse;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.StatusCodes;
import com.yushan.eagledemo.R;
import com.yushan.eagledemo.clusterutil.clustering.Cluster;
import com.yushan.eagledemo.clusterutil.clustering.ClusterItem;
import com.yushan.eagledemo.clusterutil.clustering.ClusterManager;
import com.yushan.eagledemo.track.TrackApplication;
import com.yushan.eagledemo.track.dialog.FenceCreateDialog;
import com.yushan.eagledemo.track.dialog.FenceOperateDialog;
import com.yushan.eagledemo.track.dialog.FenceSettingDialog;
import com.yushan.eagledemo.track.utils.BitmapUtil;
import com.yushan.eagledemo.track.utils.MapUtil;
import com.yushan.eagledemo.track.utils.ToastUtils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 围栏操作：负责创建、查询、更新、删除围栏，查询围栏历史报警信息
 */
public class FenceActivity extends BaseActivity implements View.OnClickListener,
        ClusterManager.OnClusterItemClickListener<FenceActivity.MyItem>,
        ClusterManager.OnClusterClickListener<FenceActivity.MyItem>,
        BaiduMap.OnMapClickListener, BaiduMap.OnMapLoadedCallback {

    private TrackApplication trackApp = null;

    /**
     * 定义点聚合管理类ClusterManager
     * <p>
     * 用于管理围栏标记点集合
     */
    private ClusterManager<MyItem> mClusterManager = null;

    /**
     * 围栏类型，默认为服务端
     */
    private FenceType fenceType = FenceType.server;

    /**
     * 围栏形状，默认为圆形
     */
    private FenceShape fenceShape = FenceShape.circle;

    /**
     * 创建、查询围栏时，输入的围栏名称
     */
    private String fenceName = null;

    /**
     * 围栏监听器
     */
    private OnFenceListener fenceListener = null;

    /**
     * 围栏设置对话框（创建、查询操作）
     */
    private FenceSettingDialog fenceSettingDialog = null;

    /**
     * 围栏设置对话框回调接口
     */
    private FenceSettingDialog.Callback settingCallback = null;

    /**
     * 围栏创建对话框
     */
    private FenceCreateDialog fenceCreateDialog = null;

    /**
     * 围栏创建对话框回调接口
     */
    private FenceCreateDialog.Callback createCallback = null;

    /**
     * 围栏操作对话框（查询历史报警、删除、更新）
     */
    private FenceOperateDialog fenceOperateDialog = null;

    /**
     * 地图工具
     */
    private MapUtil mapUtil = null;

    /**
     * 围栏覆盖物集合
     * <p>
     * key : fenceType_fenceId（如local_24, server_100）
     * value : Overlay实例
     */
    private Map<String, Overlay> overlays = new HashMap<>();

    /**
     * 围栏历史报警开始时间
     */
    private long beginTime = 0;

    /**
     * 围栏历史报警结束时间
     */
    private long endTime = 0;

    /**
     * 圆形围栏中心点坐标（地图坐标类型）
     */
    private LatLng circleCenter = null;

    /**
     * 顶点坐标（轨迹坐标类型）
     */
    private List<com.baidu.trace.model.LatLng> traceVertexes = new ArrayList<>();

    /**
     * 顶点坐标（地图坐标类型）
     */
    private List<LatLng> mapVertexes = new ArrayList<>();

    /**
     * 多边形、多段线围栏默认顶点数
     */
    private int vertexesNumber = 3;

    /**
     * 圆形围栏默认半径
     */
    private double radius = 1000;

    /**
     * 去噪（默认不去噪）
     */
    private int denoise = 0;

    /**
     * 偏离距离（默认200米）
     */
    private int offset = 200;

    /**
     * 创建多边形、多段线围栏时，临时存储顶点坐标
     */
    private Map<Integer, LatLng> tempLatLngs = new HashMap<>();

    /**
     * 创建多边形、多段线围栏时，临时存储围栏覆盖物
     * <p>
     * key : tag（请求标识）
     * value :Overlay实例
     */
    private Map<Integer, Overlay> tempOverlays = new HashMap<>();

    /**
     * 创建多边形、多段线围栏时，临时存储顶点覆盖物
     */
    private List<Overlay> tempMarks = new ArrayList<>();

    /**
     * 操作围栏时，正在操作的围栏标识，格式：fenceType_fenceId
     */
    private String fenceKey;

    /**
     * 创建多边形、多段线围栏时，正在操作的顶点下标
     */
    private int vertexIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.fence_title);
        setOnClickListener(this);
        init();
    }

    /**
     * 初始化操作
     */
    private void init() {
        initListener();
        trackApp = (TrackApplication) getApplication();
        fenceOperateDialog = new FenceOperateDialog(this);

        // 地图工具
        mapUtil = MapUtil.getInstance();
        mapUtil.init((MapView) findViewById(R.id.fence_mapView));
        mapUtil.setCenter(trackApp);
        // 设置地图加载监听
        mapUtil.baiduMap.setOnMapLoadedCallback(this);

        mClusterManager = new ClusterManager<>(this, mapUtil.baiduMap);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);

        // 设置地图监听，当地图状态发生改变时，进行点聚合运算
        mapUtil.baiduMap.setOnMapStatusChangeListener(mClusterManager);
        // 设置maker点击时的响应
        mapUtil.baiduMap.setOnMarkerClickListener(mClusterManager);

    }

    @Override
    public void onClick(View view) {
        long fenceId;
        String[] fenceKeys;
        FenceType fenceType = FenceType.server;
        switch (view.getId()) {
            // 围栏设置（创建、查询围栏）
            case R.id.btn_activity_options:
                if (null == fenceSettingDialog) {
                    fenceSettingDialog = new FenceSettingDialog(this, settingCallback);
                }
                fenceSettingDialog.show();
                break;

            // 围栏报警
            case R.id.btn_fenceOperate_alarm:
                HistoryAlarmRequest alarmRequest = null;
                List<Long> alarmFenceIds = new ArrayList<>();
                fenceId = Long.parseLong(fenceKey.split("_")[1]);
                switch (fenceType) {
                    case local:
                        alarmFenceIds.add(fenceId);
                        alarmRequest = HistoryAlarmRequest.buildLocalRequest(trackApp.getTag(),
                                trackApp.serviceId, beginTime, endTime,
                                trackApp.entityName, alarmFenceIds);
                        break;

                    case server:
                        alarmFenceIds.add(fenceId);
                        alarmRequest = HistoryAlarmRequest.buildServerRequest(trackApp.getTag(),
                                trackApp.serviceId, beginTime, endTime,
                                trackApp.entityName, alarmFenceIds, CoordType.bd09ll);
                        break;

                    default:
                        break;
                }
                trackApp.mClient.queryFenceHistoryAlarmInfo(alarmRequest, fenceListener);
                break;

            // 更新围栏
            case R.id.btn_fenceOperate_update:
                if (TextUtils.isEmpty(fenceKey)) {
                    return;
                }
                fenceKeys = fenceKey.split("_");
                fenceType = FenceType.valueOf(fenceKeys[0]);
                fenceId = Long.parseLong(fenceKeys[1]);
                UpdateFenceRequest updateRequest = null;
                if (FenceType.server == fenceType) {
                    updateRequest = UpdateFenceRequest.buildServerCircleRequest(trackApp.getTag(),
                            trackApp.serviceId, fenceId, "myFence", trackApp.entityName,
                            new com.baidu.trace.model.LatLng(40.0383290000, 116.3245630000),
                            200, 100, CoordType.bd09ll);
                } else {
                    updateRequest = UpdateFenceRequest.buildLocalCircleRequest(trackApp.getTag(),
                            trackApp.serviceId, fenceId, "myFence", trackApp.entityName,
                            new com.baidu.trace.model.LatLng(40.0383290000, 116.3245630000),
                            200, 100, CoordType.bd09ll);
                }
                trackApp.mClient.updateFence(updateRequest, fenceListener);
                break;

            // 删除围栏
            case R.id.btn_fenceOperate_delete:
                List<Long> deleteFenceIds = new ArrayList<>();
                fenceKeys = fenceKey.split("_");
                fenceType = FenceType.valueOf(fenceKeys[0]);
                fenceId = Long.parseLong(fenceKeys[1]);
                deleteFenceIds.add(fenceId);
                DeleteFenceRequest deleteRequest;
                if (FenceType.server == fenceType) {
                    deleteRequest = DeleteFenceRequest.buildServerRequest(trackApp.getTag(),
                            trackApp.serviceId, trackApp.entityName, deleteFenceIds);
                } else {
                    deleteRequest = DeleteFenceRequest.buildLocalRequest(trackApp.getTag(),
                            trackApp.serviceId, trackApp.entityName, deleteFenceIds);
                }
                trackApp.mClient.deleteFence(deleteRequest, fenceListener);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onClusterClick(Cluster<MyItem> cluster) {
        // TODO 实现删除聚合内所有围栏：从cluster中遍历MyItem，获取myItem中的key字段，从key中解析出围栏信息（围栏类型：本地、服务端；围栏编号）。
        return false;
    }

    @Override
    public boolean onClusterItemClick(MyItem item) {
        // 隐藏上一个围栏
        if (overlays.containsKey(fenceKey)) {
            overlays.get(fenceKey).setVisible(false);
        }
        fenceKey = item.getKey();
        // 显示当前围栏
        if (overlays.containsKey(fenceKey)) {
            overlays.get(fenceKey).setVisible(true);
        }
        fenceOperateDialog.showAtLocation(findViewById(R.id.layout_top),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

        // 处理PopupWindow在Android N系统上的兼容性问题
        if (Build.VERSION.SDK_INT < 24) {
            fenceOperateDialog.update(fenceOperateDialog.getWidth(), fenceOperateDialog.getHeight());
        }
        return false;
    }

    /**
     * 地图加载成功后，查询本地与服务端围栏
     */
    @Override
    public void onMapLoaded() {
        queryFenceList(FenceType.local);
        queryFenceList(FenceType.server);
    }

    /**
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {

        switch (fenceShape) {
            case circle:
                circleCenter = latLng;
                break;

            case polygon:
            case polyline:
                mapVertexes.add(latLng);
                traceVertexes.add(mapUtil.convertMap2Trace(latLng));
                vertexIndex++;
                BitmapUtil.getMark(trackApp, vertexIndex);
                OverlayOptions overlayOptions = new MarkerOptions().position(latLng)
                        .icon(BitmapUtil.getMark(trackApp, vertexIndex)).zIndex(9).draggable(true);
                tempMarks.add(mapUtil.baiduMap.addOverlay(overlayOptions));
                break;

            default:
                break;
        }

        if (null == fenceCreateDialog) {
            fenceCreateDialog = new FenceCreateDialog(this, createCallback);
        }
        if (FenceShape.circle == fenceShape || vertexIndex == vertexesNumber) {
            fenceCreateDialog.setFenceType(fenceType);
            fenceCreateDialog.setFenceShape(fenceShape);
            fenceCreateDialog.show();
        }
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    private void createFence(int tag) {
        CreateFenceRequest request = null;
        switch (fenceType) {
            case local:
                request = CreateFenceRequest.buildLocalCircleRequest(tag, trackApp.serviceId, fenceName,
                        trackApp.entityName, mapUtil.convertMap2Trace(circleCenter), radius, denoise, CoordType.bd09ll);
                break;

            case server:
                switch (fenceShape) {
                    case circle:
                        request = CreateFenceRequest.buildServerCircleRequest(tag, trackApp.serviceId, fenceName,
                                trackApp.entityName, mapUtil.convertMap2Trace(circleCenter), radius, denoise,
                                CoordType.bd09ll);
                        break;

                    case polygon:
                        request = CreateFenceRequest.buildServerPolygonRequest(tag,
                                trackApp.serviceId, fenceName, trackApp.entityName, traceVertexes, denoise,
                                CoordType.bd09ll);
                        break;

                    case polyline:
                        request = CreateFenceRequest.buildServerPolylineRequest(tag,
                                trackApp.serviceId, fenceName, trackApp.entityName, traceVertexes, offset, denoise,
                                CoordType.bd09ll);
                        break;

                    default:
                        break;
                }
                break;

            default:
                break;
        }

        trackApp.mClient.createFence(request, fenceListener);
    }

    private void queryFenceList(FenceType fenceType) {
        FenceListRequest request = null;
        switch (fenceType) {
            case local:
                request = FenceListRequest.buildLocalRequest(trackApp.getTag(),
                        trackApp.serviceId, trackApp.entityName, null);
                break;

            case server:
                request = FenceListRequest.buildServerRequest(trackApp.getTag(),
                        trackApp.serviceId, trackApp.entityName, null, CoordType.bd09ll);
                break;

            default:
                break;
        }

        trackApp.mClient.queryFenceList(request, fenceListener);
    }

    private void clearOverlay() {
        if (null != overlays) {
            for (Map.Entry<String, Overlay> entry : overlays.entrySet()) {
                entry.getValue().remove();
            }
            overlays.clear();
        }
        if (null != mClusterManager) {
            mClusterManager.clearItems();
        }
    }

    private void initListener() {

        settingCallback = new FenceSettingDialog.Callback() {
            @Override
            public void onFenceOperateCallback(FenceType fenceType, FenceShape fenceShape,
                                               String fenceName, int vertexesNumber, int operateType) {
                FenceActivity.this.fenceType = fenceType;
                FenceActivity.this.fenceShape = fenceShape;
                FenceActivity.this.fenceName = fenceName;
                FenceActivity.this.vertexesNumber = vertexesNumber;
                switch (operateType) {
                    case R.id.btn_create_fence:
                        mapUtil.baiduMap.setOnMapClickListener(FenceActivity.this);
                        break;

                    case R.id.btn_fence_list:
                        queryFenceList(fenceType);
                        break;

                    default:
                        break;
                }
            }
        };

        createCallback = new FenceCreateDialog.Callback() {

            private int tag;

            @Override
            public void onSureCallback(double radius, int denoise, int offset) {
                FenceActivity.this.radius = radius;
                FenceActivity.this.denoise = denoise;
                FenceActivity.this.offset = offset;

                OverlayOptions overlayOptions = null;
                tag = trackApp.getTag();

                if (FenceShape.circle == fenceShape) {
                    if (FenceType.local == fenceType) {
                        overlayOptions = new CircleOptions().fillColor(0x000000FF).center(circleCenter)
                                .stroke(new Stroke(5, Color.rgb(0x23, 0x19, 0xDC))).radius((int) radius);
                    } else {
                        overlayOptions = new CircleOptions().fillColor(0x000000FF).center(circleCenter)
                                .stroke(new Stroke(5, Color.rgb(0xFF, 0x06, 0x01))).radius((int) radius);
                    }
                    tempLatLngs.put(tag, circleCenter);
                } else if (FenceShape.polygon == fenceShape) {
                    overlayOptions = new PolygonOptions().points(mapVertexes)
                            .stroke(new Stroke(5, 0xFF0601)).fillColor(0xAAFFFF00);
                    tempLatLngs.put(tag, mapVertexes.get(0));
                } else if (FenceShape.polyline == fenceShape) {
                    overlayOptions = new PolylineOptions().points(mapVertexes).width(10)
                            .color(Integer.valueOf(Color.RED));
                    tempLatLngs.put(tag, mapVertexes.get(0));
                }

                tempOverlays.put(tag, mapUtil.baiduMap.addOverlay(overlayOptions));
                mapUtil.baiduMap.setOnMapClickListener(null);

                createFence(tag);
            }

            @Override
            public void onCancelCallback() {
                if (tempOverlays.containsKey(tag)) {
                    tempOverlays.get(tag).remove();
                    tempOverlays.remove(tag);
                }
                for (Overlay overlay : tempMarks) {
                    overlay.remove();
                }
                tempMarks.clear();
                vertexIndex = 0;
                mapUtil.baiduMap.setOnMapClickListener(null);
            }
        };

        fenceListener = new OnFenceListener() {
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {
                int tag = response.getTag();
                if (StatusCodes.SUCCESS == response.getStatus()) {
                    String fenceKey = response.getFenceType() + "_" + response.getFenceId();

                    Overlay overlay = tempOverlays.get(tag);
                    overlay.setVisible(false);
                    overlays.put(fenceKey, overlay);
                    tempOverlays.remove(tag);

                    Bundle bundle = new Bundle();
                    bundle.putString("fenceKey", fenceKey);

                    if (tempLatLngs.containsKey(tag)) {
                        mClusterManager.addItem(new MyItem(fenceKey, tempLatLngs.get(tag)));
                        mClusterManager.cluster();
                        tempLatLngs.remove(tag);
                    }

                    ToastUtils.showToastCentre(FenceActivity.this,
                            response.getMessage() + "," + getString(R.string.fence_operate_caption));
                } else {
                    tempOverlays.get(tag).remove();
                    tempOverlays.remove(tag);
                }
                for (Overlay overlay : tempMarks) {
                    overlay.remove();
                }
                tempMarks.clear();

            }

            @Override
            public void onUpdateFenceCallback(UpdateFenceResponse response) {

            }

            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse response) {
                ToastUtils.showToastCentre(FenceActivity.this, response.getMessage());
                List<Long> fenceIds = response.getFenceIds();
                if (null == fenceIds || fenceIds.isEmpty()) {
                    return;
                }

                FenceType fenceType = response.getFenceType();

                Iterator<Map.Entry<String, Overlay>> overlayIt = overlays.entrySet().iterator();
                while (overlayIt.hasNext()) {
                    Map.Entry<String, Overlay> entry = overlayIt.next();
                    long fenceId = Long.parseLong(entry.getKey().split("_")[1]);
                    String fenceKey = fenceType + "_" + fenceId;
                    if (fenceIds.contains(fenceId) && entry.getKey().equals(fenceKey)) {
                        entry.getValue().remove();
                        overlayIt.remove();
                        // 从聚合中删除item
                        mClusterManager.removeItem(new MyItem(fenceKey, new LatLng(0, 0)));
                    }
                }
                mapUtil.refresh();
            }

            @Override
            public void onFenceListCallback(FenceListResponse response) {
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    ToastUtils.showToastCentre(FenceActivity.this, response.getMessage());
                    return;
                }
                if (0 == response.getSize()) {
                    StringBuffer message = new StringBuffer("未查询到");
                    if (FenceType.local == response.getFenceType()) {
                        message.append("本地围栏");
                    } else {
                        message.append("服务端围栏");
                    }
                    ToastUtils.showToastCentre(FenceActivity.this, message.toString());

                    return;
                }

                ToastUtils.showToastCentre(FenceActivity.this, getString(R.string.fence_operate_caption));

                FenceType fenceType = response.getFenceType();

                List<FenceInfo> fenceInfos = response.getFenceInfos();
                List<LatLng> points = new ArrayList<>();
                String fenceKey;
                for (FenceInfo fenceInfo : fenceInfos) {
                    Bundle bundle = new Bundle();

                    Overlay overlay;
                    switch (fenceInfo.getFenceShape()) {
                        case circle:
                            CircleFence circleFence = fenceInfo.getCircleFence();
                            fenceKey = fenceType + "_" + circleFence.getFenceId();
                            bundle.putString("fenceKey", fenceKey);

                            LatLng latLng = MapUtil.convertTrace2Map(circleFence.getCenter());
                            double radius = circleFence.getRadius();
                            CircleOptions circleOptions = new CircleOptions().fillColor
                                    (0x000000FF).center(latLng)
                                    .radius((int) radius);
                            if (FenceType.local == fenceType) {
                                circleOptions.stroke(new Stroke(5, Color.rgb(0x23,
                                        0x19, 0xDC)));
                                overlay = mapUtil.baiduMap.addOverlay(circleOptions);
                                overlay.setVisible(false);
                                overlays.put(fenceKey, overlay);
                            } else {
                                circleOptions.stroke(new Stroke(5, Color.rgb(0xFF,
                                        0x06, 0x01)));
                                overlay = mapUtil.baiduMap.addOverlay(circleOptions);
                                overlay.setVisible(false);
                                overlays.put(fenceKey, overlay);
                            }
                            mClusterManager.addItem(new MyItem(fenceKey, latLng));
                            points.add(latLng);
                            break;

                        case polygon:
                            PolygonFence polygonFence = fenceInfo.getPolygonFence();
                            fenceKey = fenceType + "_" + polygonFence.getFenceId();
                            bundle.putString("fenceKey", fenceKey);
                            List<com.baidu.trace.model.LatLng> polygonVertexes = polygonFence.getVertexes();
                            List<LatLng> mapVertexes1 = new ArrayList<>();
                            for (com.baidu.trace.model.LatLng ll : polygonVertexes) {
                                mapVertexes1.add(MapUtil.convertTrace2Map(ll));
                            }
                            PolygonOptions polygonOptions = new PolygonOptions().points(mapVertexes1)
                                    .stroke(new Stroke(mapVertexes1.size(), Color.rgb(0xFF, 0x06, 0x01)))
                                    .fillColor(0x30FFFFFF);
                            overlay = mapUtil.baiduMap.addOverlay(polygonOptions);
                            overlay.setVisible(false);
                            overlays.put(fenceKey, overlay);
                            mClusterManager.addItem(new MyItem(fenceKey, mapVertexes1.get(0)));
                            points.add(mapVertexes1.get(0));
                            break;

                        case polyline:
                            PolylineFence polylineFence = fenceInfo.getPolylineFence();
                            fenceKey = fenceType + "_" + polylineFence.getFenceId();
                            bundle.putString("fenceKey", fenceKey);
                            List<com.baidu.trace.model.LatLng> polylineVertexes = polylineFence.getVertexes();
                            List<LatLng> mapVertexes2 = new ArrayList<>();
                            for (com.baidu.trace.model.LatLng ll : polylineVertexes) {
                                mapVertexes2.add(MapUtil.convertTrace2Map(ll));
                            }
                            PolylineOptions polylineOptions = new PolylineOptions().points(mapVertexes2)
                                    .color(Color.rgb(0xFF, 0x06, 0x01));
                            overlay = mapUtil.baiduMap.addOverlay(polylineOptions);
                            overlay.setVisible(false);
                            overlays.put(fenceKey, overlay);
                            mClusterManager.addItem(new MyItem(fenceKey, mapVertexes2.get(0)));
                            points.add(mapVertexes2.get(0));
                            break;

                        default:
                            break;
                    }

                }

                // 重新聚合
                mClusterManager.cluster();
                mapUtil.animateMapStatus(points);
            }

            @Override
            public void onMonitoredStatusCallback(MonitoredStatusResponse response) {

            }

            @Override
            public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse response) {

            }

            @Override
            public void onHistoryAlarmCallback(HistoryAlarmResponse response) {

                List<FenceAlarmInfo> fenceAlarmInfos = response.getFenceAlarmInfos();

                for (FenceAlarmInfo fenceAlarmInfo : fenceAlarmInfos) {
                    fenceAlarmInfo.getCurrentPoint().getLocTime();
                }

                ToastUtils.showToastCentre(FenceActivity.this, response.getMessage());
            }
        };
    }

    /**
     * 每个Marker点，包含Marker点坐标、图标、所属围栏标识
     */
    public class MyItem implements ClusterItem {

        private String mKey;

        private final LatLng mPosition;

        public MyItem(String key, LatLng latLng) {
            mKey = key;
            mPosition = latLng;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        @Override
        public String getKey() {
            return mKey;
        }

        @Override
        public BitmapDescriptor getBitmapDescriptor() {
            return BitmapUtil.bmGcoding;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MyItem myItem = (MyItem) o;

            return mKey.equals(myItem.mKey);

        }

        @Override
        public int hashCode() {
            return mKey.hashCode();
        }

        @Override
        public String toString() {
            return "MyItem{mKey='" + mKey + '\'' + ", mPosition=" + mPosition + '}';
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapUtil.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapUtil.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearOverlay();
        if (null != fenceCreateDialog) {
            fenceCreateDialog.dismiss();
            fenceCreateDialog = null;
        }
        if (null != fenceOperateDialog) {
            fenceOperateDialog.dismiss();
            fenceOperateDialog = null;
        }
        if (null != fenceSettingDialog) {
            fenceSettingDialog.dismiss();
            fenceSettingDialog = null;
        }
        if (null != mClusterManager) {
            mClusterManager.clearItems();
            mClusterManager = null;
        }
        mapUtil.clear();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_fence;
    }

}
