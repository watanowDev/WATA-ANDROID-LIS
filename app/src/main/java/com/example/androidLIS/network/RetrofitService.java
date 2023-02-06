package com.example.androidLIS.network;

import com.example.androidLIS.model.ActionInfoReqData;
import com.example.androidLIS.model.AliveReqData;
import com.example.androidLIS.model.LocationInfoReqData;
import com.example.androidLIS.model.ResponseData;

import io.reactivex.Single;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitService {

    /**
     * 지게차 연결 유무 신호
     * @param aliveData
     * @return
     */
    @POST("alive")
    public Single<Response<ResponseData>> ALIVE_MESSAGE(@Body AliveReqData aliveData);


    /**
     * 물류 이적재 데이터 전송
     * @param actionInfoData
     * @return
     */
    @POST("action")
    public Single<Response<ResponseData>> ACTION_INFO_MESSAGE(@Body ActionInfoReqData actionInfoData);


    /**
     * 지게차 위치 데이터 전송
     * @param locationInfoData
     * @return
     */
    @POST("location")
    public Single<Response<ResponseData>> LOCATION_INFO_MESSAGE(@Body LocationInfoReqData locationInfoData);


}
