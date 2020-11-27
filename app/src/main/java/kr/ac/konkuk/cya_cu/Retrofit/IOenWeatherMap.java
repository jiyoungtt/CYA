package kr.ac.konkuk.cya_cu.Retrofit;

import io.reactivex.Observable;
import kr.ac.konkuk.cya_cu.Model.WeatherResult;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface IOenWeatherMap {
    @GET("weather")
    Observable<WeatherResult> getWeatherByLatLng(@Query("lat") String lat,
                                                 @Query("lon") String lng,
                                                 @Query("appid") String appid,
                                                 @Query("units") String unit
                                                 );
}
//파싱한다