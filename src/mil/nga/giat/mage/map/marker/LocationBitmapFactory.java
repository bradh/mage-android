package mil.nga.giat.mage.map.marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.utils.MediaUtility;

import org.apache.commons.lang3.StringUtils;

import android.animation.ArgbEvaluator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class LocationBitmapFactory {

	private static Long upperBoundTimeInSeconds = 1800L;
	
//	private static ArrayList<DownloadImageTask> downloadTasks = new ArrayList<DownloadImageTask>();

	// TODO : SCOTT UFM
	private static final Integer[] colorGradient = { 0xff0000ff, 0xffffff00 ,0xffffa500 };
	
	// blue, yellow, orange
	/*private static final Integer[] colorGradient = { 0xff0000ff, 0xff0000ff, 0xff0000ff, 0xff0807f8, 0xff0f10ef, 0xff1817e7, 0xff1f1fdf, 0xff2728d7, 0xff3030d0, 0xff3737c8, 0xff3f40bf, 0xff4847b7, 0xff4f4fb0, 0xff5757a7, 0xff5f5fa0, 0xff676797, 0xff706f8f, 0xff777787, 0xff7f7f7f, 0xff878878,
			0xff8f9070, 0xff989767, 0xff9f9f60, 0xffa7a758, 0xffafb050, 0xffb8b848, 0xffbfbf40, 0xffc7c738, 0xffcfcf30, 0xffd8d828, 0xffdfe020, 0xffe7e718, 0xffefef10, 0xfff7f708, 0xffffff00, 0xffffff00, 0xfffffc00, 0xfffffa00, 0xfffff600, 0xfffff400, 0xfffff100, 0xffffee00, 0xffffeb00, 0xffffe800,
			0xffffe600, 0xffffe300, 0xffffe100, 0xffffdd00, 0xffffdb00, 0xffffd700, 0xffffd500, 0xffffd200, 0xffffcf00, 0xffffcd00, 0xffffca00, 0xffffc700, 0xffffc400, 0xffffc100, 0xffffbf00, 0xffffbc00, 0xffffb900, 0xffffb600, 0xffffb300, 0xffffb000, 0xffffad00, 0xffffaa00, 0xffffa500 };*/
	// hsv spectrum
	private static final Integer[] colorGradient1 = { 0xff0000ff, 0xff0a00ff, 0xff1500ff, 0xff1f00ff, 0xff2a00ff, 0xff3500ff, 0xff3f00ff, 0xff4a00ff, 0xff5400ff, 0xff5f00ff, 0xff6900ff, 0xff7400ff, 0xff7e00ff, 0xff8900ff, 0xff9300ff, 0xff9e00ff, 0xffa800ff, 0xffb300ff, 0xffbe00ff, 0xffc800ff,
			0xffd300ff, 0xffdd00ff, 0xffe800ff, 0xfff200ff, 0xfffc00ff, 0xffff00f6, 0xffff00ec, 0xffff00e1, 0xffff00d7, 0xffff00cc, 0xffff00c2, 0xffff00b7, 0xffff00ad, 0xffff00a2, 0xffff0098, 0xffff008d, 0xffff0083, 0xffff0078, 0xffff006d, 0xffff0063, 0xffff0058, 0xffff004e, 0xffff0044, 0xffff0039,
			0xffff002e, 0xffff0023, 0xffff0019, 0xffff000e, 0xffff0004, 0xffff0700, 0xffff1100, 0xffff1b00, 0xffff2700, 0xffff3100, 0xffff3c00, 0xffff4600, 0xffff5100, 0xffff5b00, 0xffff6600, 0xffff7000, 0xffff7b00, 0xffff8500, 0xffff9000, 0xffff9a00, 0xffffa500 };

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Location location, User user) {
		Bitmap finalBitmap = null;
		Bitmap dotBitmap = createDot(context, location, user);
		Log.d("LocationBitmapFactory", "Drawing the bitmap for user " + user.getUsername());
		if (user.getLocalIconPath() != null) {
			finalBitmap = combineIconAndDot(dotBitmap.copy(Bitmap.Config.ARGB_8888, true), BitmapFactory.decodeFile(user.getLocalIconPath()));
//			m.setIcon(BitmapDescriptorFactory.fromBitmap(combined));
		//} else if (user.getIconUrl() != null) {
			// if there is supposed to be an icon but we don't have it, go get it
//			if (ConnectivityUtility.isOnline(context)) {
//				String token = PreferenceHelper.getInstance(context).getValue(R.string.tokenKey);
//				DownloadImageTask task = new DownloadImageTask(dotBitmap.copy(Bitmap.Config.ARGB_8888, true), m, user, context);
//				downloadTasks.add(task);
//				task.execute(user.getIconUrl() + "?access_token=" + token);
//				m.setIcon(BitmapDescriptorFactory.fromBitmap(dotBitmap));
//			}
		} else {
			finalBitmap = dotBitmap;
//			m.setIcon(BitmapDescriptorFactory.fromBitmap(finalBitmap));
		}


		return finalBitmap;
	}
	
//	public static void cancelDownloadTasks() {
//		Log.d("LocationBitmapFactory", "Cancelling " + downloadTasks.size() + " tasks");
//		for (DownloadImageTask task : downloadTasks) {
//			task.cancel(true);
//			task = null;
//		}
//		downloadTasks.clear();
//		Log.d("LocationBitmapFactory", "Cleared all of the download tasks");
//	}
	
	private static Bitmap combineIconAndDot(Bitmap dot, Bitmap icon) {
		Bitmap combined = Bitmap.createBitmap(96, 127, Config.ARGB_8888);
		Canvas c = new Canvas(combined);
		
		c.drawBitmap(dot, (96-dot.getWidth())/2, 95, null);
		
		Bitmap roundedProfile = MediaUtility.resizeAndRoundCorners(icon, 96);
		
		c.drawBitmap(roundedProfile, (96-roundedProfile.getWidth())/2, 0, null);
		
		return combined;
	}
	
//	private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
//	    Marker marker;
//	    Bitmap dotBitmap;
//	    User user;
//	    Context context;
//
//	    public DownloadImageTask(Bitmap dotBitmap, Marker m, User user, Context context) {
//	        this.marker = m;
//	        this.dotBitmap = dotBitmap;
//	        this.user = user;
//	        this.context = context;
//	    }
//
//	    protected Bitmap doInBackground(String... urls) {
//	        String urldisplay = urls[0];
//	        Bitmap icon = null;
//	        try {
//	        InputStream in = new java.net.URL(urldisplay).openStream();
//            icon = BitmapFactory.decodeStream(in);
//	            
//	        } catch (Exception e) {
//	            Log.e("Error", e.getMessage());
//	            e.printStackTrace();
//	        }
//	        return icon;
//	    }
//
//	    protected void onPostExecute(Bitmap icon) {
//	    	
//	    	FileOutputStream out = null;
//    		try {
//    			String localPath = MediaUtility.getUserIconDirectory() + "/" + user.getId();
//    		    out = new FileOutputStream(localPath);
//    		    icon.compress(Bitmap.CompressFormat.PNG, 90, out);
//    		    user.setLocalIconPath(localPath);
//    		    UserHelper.getInstance(context).update(user);
//    		} catch (Exception e) {
//    		    e.printStackTrace();
//    		} finally {
//    		    try {
//    		        if (out != null) {
//    		            out.close();
//    		        }
//    		    } catch (IOException e) {
//    		        e.printStackTrace();
//    		    }
//    		}
//	    	
//	    	Bitmap combined = combineIconAndDot(dotBitmap.copy(Bitmap.Config.ARGB_8888, true), icon.copy(Bitmap.Config.ARGB_8888, true));
//	    	if (marker != null && combined != null && !this.isCancelled()) {
//				marker.setIcon(BitmapDescriptorFactory.fromBitmap(combined));
//			}
//	    }
//	}

	public static BitmapDescriptor bitmapDescriptor(Context context, Location location, User user) {
		Bitmap bitmap = bitmap(context, location, user);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	
	public static BitmapDescriptor dotBitmapDescriptor(Context context, Location location, User user) {
		Bitmap bitmap = createDot(context, location, user);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	
	public static Bitmap createDot(Context context, Location location, User user) {
		Bitmap dotBitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inDensity = 480;
		options.inTargetDensity = context.getResources().getDisplayMetrics().densityDpi;

		try {
			Long interval = (System.currentTimeMillis() - location.getTimestamp().getTime()) / 1000l;
			// max out at 30 minutes
			interval = Math.min(interval, upperBoundTimeInSeconds);

			// upper bound in minutes
			Double u = upperBoundTimeInSeconds.doubleValue() / 60.0;

			// non-linear lookup
			// 0 mins = blue
			// 10 mins = yellow
			// 30 mins = orange
			Double gradientIndexDecimalNormalized = -0.25 + Math.sqrt((u * u) + (u * 24 * (interval.doubleValue() / 60.0))) / (4 * u);
			// between 0 and 1
			gradientIndexDecimalNormalized = Math.min(Math.max(0.0, gradientIndexDecimalNormalized), 1.0);

			// find the index into the gradient
			int gradientIndex = (int) (gradientIndexDecimalNormalized * (double) (colorGradient.length - 1));

			// linearly interpolate between the gradient index
			Integer COLOR1 = colorGradient[gradientIndex];
			Integer COLOR2 = colorGradient[Math.min(gradientIndex + 1, colorGradient.length - 1)];
			
			// TODO : SCOTT UFM
			int color = (Integer) new ArgbEvaluator().evaluate(gradientIndexDecimalNormalized.floatValue(), COLOR1, COLOR1);

			// use a invert filter to swap black and white colors in the bitmap. This will preserve the original black
			float[] colorMatrix_Negative = { -1.0f, 0, 0, 0, 255, 0, -1.0f, 0, 0, 255, 0, 0, -1.0f, 0, 255, 0, 0, 0, 1.0f, 0 };

			// make a mutable copy of the bitmap
			Bitmap bitmapFile = BitmapFactory.decodeStream(context.getAssets().open("dots/black_dot.png"), null, options);
			dotBitmap = bitmapFile.copy(Bitmap.Config.ARGB_8888, true);

			Canvas myCanvas = new Canvas(dotBitmap);
			// invert the gradient first
			Paint gradientPaint = new Paint();
			gradientPaint.setColorFilter(new LightingColorFilter(Color.WHITE, 0xFFFFFFFF - color));
			myCanvas.drawBitmap(dotBitmap, 0, 0, gradientPaint);
			// invert the entire image second
			Paint negativePaint = new Paint();
			negativePaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(colorMatrix_Negative)));
			myCanvas.drawBitmap(dotBitmap, 0, 0, negativePaint);
		} catch (IOException e1) {
			try {
				dotBitmap = BitmapFactory.decodeStream(context.getAssets().open("dots/maps_dav_bw_dot.png"), null, options);
			} catch (IOException e2) {
			}
		}
		return dotBitmap;
	}

	/**
	 * @deprecated No long using hard coded assets.
	 * 
	 * @param location
	 * @return
	 */
	private static String getAsset(Location location) {
		if (location == null) {
			return null;
		}

		Collection<String> paths = new ArrayList<String>();
		paths.add("people");

		Long interval = System.currentTimeMillis() - location.getTimestamp().getTime();
		if (interval < 600000) { // 10 minutes
			paths.add("low");
		} else if (interval < 1800000) { // 30 minutes
			paths.add("medium");
		} else {
			paths.add("high");
		}

		paths.add("person");
		return StringUtils.join(paths, "/") + ".png";
	}
}