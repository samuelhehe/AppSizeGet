package com.samuel.appsizeget;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnItemClickListener {
	private static String TAG = "APP_SIZE";

	private ListView listview = null;
	private List<AppInfo> mlistAppInfo = null;
	LayoutInflater infater = null;
	private long cachesize;
	private long datasize;
	private long codesize;
	private long totalsize;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browse_app_list);
		listview = (ListView) findViewById(R.id.listviewApp);
		mlistAppInfo = new ArrayList<AppInfo>();
		queryAppInfo();
		BrowseApplicationInfoAdapter browseAppAdapter = new BrowseApplicationInfoAdapter(
				this, mlistAppInfo);
		listview.setAdapter(browseAppAdapter);
		listview.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {

		try {
			queryPacakgeSize2(mlistAppInfo.get(position).getPkgName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		infater = (LayoutInflater) MainActivity.this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View dialog = infater.inflate(R.layout.dialog_app_size, null);
		TextView tvcachesize = (TextView) dialog.findViewById(R.id.tvcachesize);
		TextView tvdatasize = (TextView) dialog.findViewById(R.id.tvdatasize);
		TextView tvcodesize = (TextView) dialog.findViewById(R.id.tvcodesize);
		TextView tvtotalsize = (TextView) dialog.findViewById(R.id.tvtotalsize);
		tvcachesize.setText(formateFileSize(cachesize));
		tvdatasize.setText(formateFileSize(datasize));
		tvcodesize.setText(formateFileSize(codesize));
		tvtotalsize.setText(formateFileSize(totalsize));

		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setView(dialog);
		builder.setTitle(mlistAppInfo.get(position).getAppLabel() + "应用详情");
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				dialog.cancel();
			}

		});
		builder.create().show();
	}

	public void queryPacakgeSize(String pkgName) throws Exception {
		if (pkgName != null) {
			PackageManager pm = getPackageManager();
			try {
				Method getPackageSizeInfo = pm.getClass().getDeclaredMethod(
						"getPackageSizeInfo", String.class,
						IPackageStatsObserver.class);
				getPackageSizeInfo.invoke(pm, pkgName, new PkgSizeObserver());
			} catch (Exception ex) {
				Log.e(TAG, "NoSuchMethodException");
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	public void queryPacakgeSize2(String pkgName) throws Exception {
		if (pkgName != null) {
			PackageManager pm = getPackageManager();
			try {
				Method getPackageSizeInfo = pm.getClass().getDeclaredMethod(
						"getPackageSizeInfo", String.class, int.class,
						IPackageStatsObserver.class);
				getPackageSizeInfo.invoke(pm, pkgName,
						Process.myUid() / 100000, new PkgSizeObserver());
			} catch (Exception ex) {
				Log.e(TAG, "NoSuchMethodException");
				ex.printStackTrace();
				throw ex;
			}
		}
	}

	public class PkgSizeObserver extends IPackageStatsObserver.Stub {
		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
				throws RemoteException {

			cachesize = pStats.cacheSize;
			datasize = pStats.dataSize;
			codesize = pStats.codeSize;
			totalsize = cachesize + datasize + codesize;
			Log.i(TAG, "cachesize--->" + cachesize + " datasize---->"
					+ datasize + " codeSize---->" + codesize);
		}
	}

	private String formateFileSize(long size) {
		return Formatter.formatFileSize(MainActivity.this, size);
	}

	public void queryAppInfo() {
		PackageManager pm = this.getPackageManager(); 
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> resolveInfos = pm
				.queryIntentActivities(mainIntent, 0);
		Collections.sort(resolveInfos,
				new ResolveInfo.DisplayNameComparator(pm));
		if (mlistAppInfo != null) {
			mlistAppInfo.clear();
			for (ResolveInfo reInfo : resolveInfos) {
				String activityName = reInfo.activityInfo.name;
				String pkgName = reInfo.activityInfo.packageName;
				String appLabel = (String) reInfo.loadLabel(pm);
				Drawable icon = reInfo.loadIcon(pm);
				Intent launchIntent = new Intent();
				launchIntent.setComponent(new ComponentName(pkgName,
						activityName));
				AppInfo appInfo = new AppInfo();
				appInfo.setAppLabel(appLabel);
				appInfo.setPkgName(pkgName);
				appInfo.setAppIcon(icon);
				appInfo.setIntent(launchIntent);
				mlistAppInfo.add(appInfo); // ������б���
			}
		}
	}
}