package com.mycoolweather.app.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.mycoolweather.app.db.CoolWeatherDB;
import com.mycoolweather.app.model.City;
import com.mycoolweather.app.model.County;
import com.mycoolweather.app.model.Province;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Utility {
	//解析省级数据
	public synchronized static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB, String response) {
		if(!TextUtils.isEmpty(response)){
			String[] allProvinces = response.split(",");
			if(allProvinces != null && allProvinces.length > 0){
				for(String p:allProvinces){
					String[] array = p.split("\\|");
					Province province = new Province();
					province.setProvinceName(array[1]);
					province.setProvinceCode(array[0]);
					coolWeatherDB.saveProvince(province);
				}
			    return true;
		    }
		}
		return false;
	}
	
	//解析省级数据
		public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB, String response, int provinceId) {
			if(!TextUtils.isEmpty(response)){
				String[] allCities = response.split(",");
				if(allCities != null && allCities.length > 0){
					for(String c:allCities){
						String[] array = c.split("\\|");
						City city = new City();
						city.setCityName(array[1]);
						city.setCityCode(array[0]);
						city.setProvinceId(provinceId);
						coolWeatherDB.saveCity(city);
					}
				return true;
			}
				}
			return false;
		}
		
		//解析省级数据
		public static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB, String response, int cityId) {
			if(!TextUtils.isEmpty(response)){
				String[] allCounties = response.split(",");
				if(allCounties != null && allCounties.length > 0){
					for(String p:allCounties){
						String[] array = p.split("\\|");
						County county = new County();
						county.setCountyName(array[1]);
						county.setCountyCode(array[0]);
						county.setCityId(cityId);
						coolWeatherDB.saveCounty(county);
					}
				return true;
			}
			}
			return false;
		}
		
		//解析服务器返回的天气信息，该格式是JSON,并且将解析出来的数据存放到数据库中
		public static void handleWeatherResponse(Context context, String response){
			try{
				JSONObject jsonObject = new JSONObject(response);
				JSONObject weatherInfo = jsonObject.getJSONObject("weatherinfo");
				String cityName = weatherInfo.getString("city");
				String weatherCode = weatherInfo.getString("cityid");
				String temp1 = weatherInfo.getString("temp1");
				String temp2 = weatherInfo.getString("temp2");
				String weatherDesp = weatherInfo.getString("weather");
				String publishTime = weatherInfo.getString("ptime");
				saveWeatherInfo(context, cityName, weatherCode, temp1, temp2, weatherDesp, publishTime);
			}catch(JSONException e){
				e.printStackTrace();
			}
			
		}
		
        //将天气信息存放到SharedPreferences文件中
		private static void saveWeatherInfo(Context context, String cityName,
				String weatherCode, String temp1, String temp2,
				String weatherDesp, String publishTime) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日",Locale.CHINA);
			SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putBoolean("city_selected", true);
			editor.putString("city_name", cityName);
			editor.putString("weather_code", weatherCode);
			editor.putString("temp1", temp1);
			editor.putString("temp2", temp2);
			editor.putString("weather_desp", weatherDesp);
			editor.putString("publish_time", publishTime);
			editor.putString("current_date", sdf.format(new Date()));
			editor.commit();
		}

}
