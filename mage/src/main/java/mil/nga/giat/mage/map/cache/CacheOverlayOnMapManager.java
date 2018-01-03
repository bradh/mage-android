package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class CacheOverlayOnMapManager implements CacheManager.CacheOverlaysUpdateListener {

    private final CacheManager cacheManager;
    private final GoogleMap map;
    private final Map<Class<? extends CacheProvider>, CacheProvider> providers = new HashMap<>();
    private final List<CacheOverlayOnMap> overlays = new ArrayList<>();

    public CacheOverlayOnMapManager(CacheManager cacheManager, GoogleMap map) {
        this.cacheManager = cacheManager;
        this.map = map;
        for (CacheProvider provider : cacheManager.getProviders()) {
            providers.put(provider.getClass(), provider);
        }
    }

    @Override
    public void onCacheOverlaysUpdated(CacheManager.CacheOverlayUpdate update) {
    }

    public CacheOverlayOnMap createCacheOverlayOnMap(CacheOverlay cache) {
        CacheProvider provider = providers.get(cache.getType());
        CacheOverlayOnMap overlay = provider.createOverlayOnMapFromCache(cache, map);
        overlays.add(overlay);
        return overlay;
    }

    private CacheProvider providerForOverlay(CacheOverlayOnMap overlay) {
        return providers.get(overlay.getCache().getType());
    }
}
