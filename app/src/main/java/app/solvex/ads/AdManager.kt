package app.solvex.ads

import android.app.Activity
import android.content.Context
import app.solvex.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdManager {
    // Debug builds always use Google's public test ad units so development on your
    // own phone never serves (or clicks) real ads — that counts as invalid traffic.
    private val TEST = BuildConfig.DEBUG

    val BANNER_AD_UNIT_ID =
        if (TEST) "ca-app-pub-3940256099942544/6300978111" else "ca-app-pub-2498267529185476/1209566467"
    val INTERSTITIAL_AD_UNIT_ID =
        if (TEST) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-2498267529185476/6793065636"
    val REWARDED_AD_UNIT_ID =
        if (TEST) "ca-app-pub-3940256099942544/5224354917" else "ca-app-pub-2498267529185476/1373217999"

    // Interstitial frequency cap: at most one every 2 minutes, and never on the first Play tap.
    private const val MIN_INTER_INTERVAL_MS = 120_000L
    private var lastInterShownMs = 0L
    private var interRequests = 0

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun init(context: Context) {
        MobileAds.initialize(context) {
            loadInterstitial(context)
            loadRewarded(context)
        }
    }

    fun loadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun loadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        interRequests++
        val ad = interstitialAd
        val now = System.currentTimeMillis()
        if (ad == null || interRequests < 2 || now - lastInterShownMs < MIN_INTER_INTERVAL_MS) {
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastInterShownMs = System.currentTimeMillis()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial(activity)
                onDone()
            }
        }
        ad.show(activity)
    }

    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
                if (!earned) onUnavailable()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onUnavailable()
            }
        }
        ad.show(activity) {
            earned = true
            onReward()
        }
    }
}
