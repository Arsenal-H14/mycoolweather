package com.mycoolweather.app.activity;

import com.mycoolweather.app.R;
import com.mycoolweather.app.model.City;
import com.mycoolweather.app.model.Province;
import com.mycoolweather.app.service.AutoUpdateService;
import com.mycoolweather.app.util.HttpCallbackListener;
import com.mycoolweather.app.util.HttpUtil;
import com.mycoolweather.app.util.Utility;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WeatherActivity extends Activity implements OnClickListener{
	private LinearLayout weatherInfoLayout;
	
	/**
	 * ������ʾ������
	 */
	private TextView cityNameText;
	/**
	 * ������ʾ����ʱ��
	 */
	private TextView publishText;
	/**
	 * ������ʾ����������Ϣ
	 */
	private TextView weatherDespText;
	/**
	 * ������ʾ����1
	 */
	private TextView temp1Text;
	/**
	 * ������ʾ����2
	 */
	private TextView temp2Text;
	/**
	 * ������ʾ��ǰ����
	 */
	private TextView currentDateText;
	
	private Button switchCity;
	private Button refreshWeather;
	
	private City selectedCity;
	private Province selectedProvince;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.weather_layout);
		
		weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
		cityNameText = (TextView) findViewById(R.id.city_name);
		publishText = (TextView) findViewById(R.id.publish_text);
		weatherDespText = (TextView) findViewById(R.id.weather_desp);
		temp1Text = (TextView) findViewById(R.id.temp1);
		temp2Text = (TextView) findViewById(R.id.temp2);
		currentDateText = (TextView) findViewById(R.id.current_date);
		switchCity = (Button) findViewById(R.id.switch_city);
		refreshWeather = (Button) findViewById(R.id.refresh_weather);
		
		/*
		 * ����3�еõ�ChooseAreaActivity���ݹ����Ĳ���ֵ������countyCode���ڱ����ʹ�ã���selectedProvince��selectedCity
		 * �������ڱ����ʹ�ã���ֻ�ǵ����·��ؼ���ʱ�����������󷵻ص�ChooseAreaActivity��С�
		 * */
		String countyCode = getIntent().getStringExtra("county_code");
		selectedCity = (City) getIntent().getSerializableExtra("City");
		selectedProvince = (Province) getIntent().getSerializableExtra("Province");
		
		switchCity.setOnClickListener(this);
		refreshWeather.setOnClickListener(this);
		
		/*
		 * countyCodeΪnullʱ��ֱ����SharedPreference�ļ��е�������ʾ������Ϣ
		 * */
		if(!TextUtils.isEmpty(countyCode)){
			publishText.setText("ͬ����...");
			weatherInfoLayout.setVisibility(View.INVISIBLE);
			cityNameText.setVisibility(View.INVISIBLE);
			queryWeatherCode(countyCode);
		}else{
			showWeather();
		}
		
	}	

	private void queryWeatherCode(String countyCode) {
		String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
		queryFromServer(address, "countyCode");
		
	} 
		
	private void queryFromServer(final String address,  final String type) {
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener(){
			@Override
			public void onFinish(final String response){				
				if("countyCode".equals(type)){
					if(!TextUtils.isEmpty(response)){
						String[] array = response.split("\\|");
						
						if(array != null && array.length == 2){
							String weatherCode = array[1];
							queryWeatherInfo(weatherCode);
						}		
					}
				}else if("weatherCode".equals(type)){
					Utility.handleWeatherResponse(WeatherActivity.this, response);
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							showWeather();						
						}
					});
				}
			}
			
			@Override
			public void onError(Exception e){
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						publishText.setText("ͬ��ʧ��");
					}
				});
			}	
		});
		
	}

	protected void queryWeatherInfo(String weatherCode) {
		String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
		queryFromServer(address, "weatherCode");	
	}

	private void showWeather() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cityNameText.setText(prefs.getString("city_name", ""));
		temp1Text.setText(prefs.getString("temp1", ""));
		temp2Text.setText(prefs.getString("temp2", ""));
		weatherDespText.setText(prefs.getString("weather_desp", ""));
		publishText.setText("����" + prefs.getString("publish_time", "") + "����");
		currentDateText.setText(prefs.getString("current_date", ""));
		weatherInfoLayout.setVisibility(View.VISIBLE);
		cityNameText.setVisibility(View.VISIBLE);
		Intent intent = new Intent(this, AutoUpdateService.class);
		startService(intent);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		
		//��������from_weather_activity������ChooseAreaActivity��Ҫ����ת����
		case R.id.switch_city:
			Intent intent = new Intent(this, ChooseAreaActivity.class);
			intent.putExtra("from_weather_activity", true);
			startActivity(intent);
			finish();
			break;
			
			//��SharedPreferences�ļ��еõ���ǰ�����е�weather_code��ֱ�ӾͿ�������ˢ�µ�ǰ���е�����
		case R.id.refresh_weather:
			publishText.setText("ͬ����...");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String weatherCode = prefs.getString("weather_code", "");
			if(!TextUtils.isEmpty(weatherCode))
				queryWeatherInfo(weatherCode);
			break;

		default:
			break;
		}
		
	}
	
	@Override
	public void onBackPressed() {
		/*
		 *���·��ؼ��󣬱������ selectedProvince��selectedCity��from_weather_activity��ChooseAreaActivity���
		 * ���У�from_weather_activity�������Ǹ���ChooseAreaActivity���Ҫ�ٴ���ת������У��������밴��switch_city
		 * ���from_weather_activity��ͬ��
		 * */
		Intent intent = new Intent(this, ChooseAreaActivity.class);		
		intent.putExtra("from_weather_activity", true);		
		Bundle mBundle = new Bundle();
		mBundle.putSerializable("Province", selectedProvince);			
		mBundle.putSerializable("City", selectedCity);
		intent.putExtras(mBundle);
		startActivity(intent);
		finish();
	}
}