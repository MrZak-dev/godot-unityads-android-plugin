package org.godotengine.godotunityadsplugin;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.util.Set;

public class GodotUnityAds extends GodotPlugin implements View.OnClickListener {
    
    private static final String TAG  = "GodotUnityAds";


    private static Activity activity;
    private static Context context;

    private static String interstitialId;
    private static String rewardedId;
    private static String bannerId;

    private static FrameLayout layout = null;
    private static BannerView banner;
    private static UnityShowListener showListener;


    public GodotUnityAds(Godot godot) {
        super(godot);
        GodotUnityAds.activity = getActivity();
        GodotUnityAds.context = godot.getContext();
    }

    @Nullable
    @Override
    public View onMainCreate(Activity activity) {
        layout = new FrameLayout(activity);
        return layout;
    }

    @UsedByGodot
    public void initialize(String gameId , String interstitialId , String rewardedId ,
                           String bannerId , boolean testMode){
        
        GodotUnityAds.interstitialId = interstitialId;
        GodotUnityAds.rewardedId = rewardedId;
        GodotUnityAds.bannerId  = bannerId;

        GodotUnityAds.showListener = new UnityShowListener();

        final UnityAdsListener adsListener = new UnityAdsListener();

        UnityAds.addListener(adsListener);
        UnityAds.initialize(GodotUnityAds.context,gameId,testMode);
    }

    @UsedByGodot
    public boolean isAdLoaded(String unitId){
        return UnityAds.getPlacementState(unitId).equals(UnityAds.PlacementState.READY);
    }

    @UsedByGodot
    public void showInterstitial(){
        if(UnityAds.getPlacementState(GodotUnityAds.interstitialId) != UnityAds.PlacementState.READY){
            return;
        }
        UnityAds.show(GodotUnityAds.activity,GodotUnityAds.interstitialId,GodotUnityAds.showListener);
    }

    @UsedByGodot
    public void showRewarded(){
        if(UnityAds.getPlacementState(GodotUnityAds.rewardedId) != UnityAds.PlacementState.READY){
            return;
        }
        UnityAds.show(GodotUnityAds.activity,GodotUnityAds.rewardedId,GodotUnityAds.showListener);
    }

    @UsedByGodot
    public void loadBanner(boolean isTop){
        GodotUnityAds.activity.runOnUiThread(() -> {
            final UnityBannerListener bannerListener = new UnityBannerListener();
            banner = new BannerView(GodotUnityAds.activity,GodotUnityAds.bannerId,
                    UnityBannerSize.getDynamicSize(GodotUnityAds.context));
            banner.setListener(bannerListener);
            banner.load();

            if(banner.getParent() != null){
                ((ViewGroup) banner.getParent()).removeView(banner);
            }

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    isTop ? Gravity.TOP : Gravity.BOTTOM);
            layout.addView(banner,0,layoutParams);
        });
    }

    @UsedByGodot
    public void showBanner(){
        if(banner == null){
            return;
        }
        GodotUnityAds.activity.runOnUiThread(() -> {
            if(banner.getVisibility() == View.INVISIBLE){
                banner.setVisibility(View.VISIBLE);
            }
        });

    }

    @UsedByGodot
    public void hideBanner(){
        if(banner == null){
            return;
        }
        GodotUnityAds.activity.runOnUiThread(() -> {
            if(banner.getVisibility() == View.VISIBLE){
                banner.setVisibility(View.INVISIBLE);
            }
        });

    }



    //region Listeners
    private class UnityAdsListener implements IUnityAdsListener {

        @Override
        public void onUnityAdsReady(String unitId) {
            Log.d(TAG, "onUnityAdsReady: " + unitId);

           if(unitId.equals(GodotUnityAds.interstitialId)){
               emitSignal("on_interstitial_loaded");
           }else if(unitId.equals(GodotUnityAds.rewardedId)){
               emitSignal("on_rewarded_loaded");
           }
        }

        @Override
        public void onUnityAdsStart(String unitId) {
            Log.d(TAG, "onUnityAdsStart: " + unitId);

            if(unitId.equals(GodotUnityAds.interstitialId)){
                emitSignal("on_interstitial_opened");
            }else if(unitId.equals(GodotUnityAds.rewardedId)){
                emitSignal("on_rewarded_opened");
            }
        }

        @Override
        public void onUnityAdsFinish(String unitId, UnityAds.FinishState finishState) {
            Log.d(TAG, "onUnityAdsFinish: " + unitId);

            if(unitId.equals(GodotUnityAds.interstitialId)){
                emitSignal("on_interstitial_closed");
            }else if(unitId.equals(GodotUnityAds.rewardedId)){
                if(finishState == UnityAds.FinishState.SKIPPED){
                    emitSignal("on_rewarded_closed");
                }else if(finishState == UnityAds.FinishState.COMPLETED){
                    emitSignal("on_rewarded");
                }
            }
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String unitId) {
            Log.d(TAG, "onUnityAdsError: " + unitId);
        }
    }

    private class UnityShowListener implements  IUnityAdsShowListener{


        @Override
        public void onUnityAdsShowFailure(String unitId, UnityAds.UnityAdsShowError unityAdsShowError, String s1) {
            Log.d(TAG, "onUnityAdsShowFailure: " + unitId );
        }

        @Override
        public void onUnityAdsShowStart(String unitId) {
            Log.d(TAG, "onUnityAdsShowStart: " + unitId);
        }

        @Override
        public void onUnityAdsShowClick(String unitId) {
            Log.d(TAG, "onUnityAdsShowClick: " + unitId);
        }

        @Override
        public void onUnityAdsShowComplete(String unitId, UnityAds.UnityAdsShowCompletionState unityAdsShowCompletionState) {
            Log.d(TAG, "onUnityAdsShowComplete: " + unitId);
        }
    }


    private class UnityBannerListener implements BannerView.IListener {


        @Override
        public void onBannerLoaded(BannerView bannerView) {
            Log.d(TAG, "onBannerLoaded: ");
            emitSignal("on_banner_loaded");
        }

        @Override
        public void onBannerClick(BannerView bannerView) {

        }

        @Override
        public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {

        }

        @Override
        public void onBannerLeftApplication(BannerView bannerView) {

        }
    }


    //endregion



    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();

        // interstitial
        signals.add(new SignalInfo("on_interstitial_loaded"));
        signals.add(new SignalInfo("on_interstitial_opened"));
        signals.add(new SignalInfo("on_interstitial_closed"));

        // Rewarded
        signals.add(new SignalInfo("on_rewarded_loaded"));
        signals.add(new SignalInfo("on_rewarded_opened"));
        signals.add(new SignalInfo("on_rewarded_closed"));
        signals.add(new SignalInfo("on_rewarded"));


        signals.add(new SignalInfo("on_banner_loaded"));

        return signals;
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "GodotUnityAds";
    }

    @Override
    public void onClick(View view) {

    }
}
