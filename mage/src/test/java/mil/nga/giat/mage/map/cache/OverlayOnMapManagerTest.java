package mil.nga.giat.mage.map.cache;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bechte.junit.runners.context.HierarchicalContextRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@RunWith(HierarchicalContextRunner.class)
public class OverlayOnMapManagerTest implements CacheManager.CreateUpdatePermission {

    static class TestOverlayOnMap extends OverlayOnMapManager.OverlayOnMap {

        boolean visible;
        boolean onMap;
        boolean disposed;
        int zIndex;

        TestOverlayOnMap(OverlayOnMapManager manager) {
            manager.super();
        }

        @Override
        protected void addToMap() {
            onMap = true;
        }

        @Override
        protected void removeFromMap() {
            onMap = false;
        }

        @Override
        protected void show() {
            visible = true;
        }

        @Override
        protected void hide() {
            visible = false;
        }

        @Override
        protected void setZIndex(int z) {
            zIndex = z;
        }

        int getZIndex() {
            return zIndex;
        }

        @Override
        protected void zoomMapToBoundingBox() {

        }

        @Override
        protected boolean isOnMap() {
            return onMap;
        }

        @Override
        protected boolean isVisible() {
            return visible;
        }


        @Override
        protected String onMapClick(LatLng latLng, MapView mapView) {
            return null;
        }

        @Override
        protected void dispose() {
            disposed = true;
        }

        OverlayOnMapManager.OverlayOnMap visible(boolean x) {
            if (x) {
                show();
            }
            else {
                hide();
            }
            return this;
        }

        OverlayOnMapManager.OverlayOnMap onMap(boolean x) {
            if (x) {
                addToMap();
            }
            else {
                removeFromMap();
            }
            return this;
        }
    }

    static OverlayOnMapManager.OverlayOnMap mockOverlayOnMap(OverlayOnMapManager overlayManager) {
        return mock(OverlayOnMapManager.OverlayOnMap.class, withSettings().useConstructor(overlayManager));
    }

    @SafeVarargs
    static <T> Set<T> setOf(T... things) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(things)));
    }


    CacheManager cacheManager;
    CacheOverlayTest.TestCacheProvider1 provider1;
    CacheOverlayTest.TestCacheProvider2 provider2;
    List<CacheProvider> providers;

    @Before
    public void setup() {

        provider1 = mock(CacheOverlayTest.TestCacheProvider1.class);
        provider2 = mock(CacheOverlayTest.TestCacheProvider2.class);
        cacheManager = mock(CacheManager.class, withSettings().useConstructor(new CacheManager.Config().updatePermission(this)));
        providers = Arrays.asList(provider1, provider2);
    }

    @Test
    public void listensToCacheManagerUpdates() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        verify(cacheManager).addUpdateListener(overlayManager);
    }

    @Test
    public void addsOverlaysFromAddedCaches() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1, overlay2));
        Set<MapCache> added = setOf(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, added, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(mockOverlayOnMap(overlayManager));
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(mockOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));
    }

    @Test
    public void removesOverlaysFromRemovedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1, overlay2));

        when(cacheManager.getCaches()).thenReturn(setOf(mapCache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));

        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet(), setOf(mapCache));
        overlayManager.onCacheOverlaysUpdated(update);

        assertTrue(overlayManager.getOverlaysInZOrder().isEmpty());
    }

    @Test
    public void removesOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1, overlay2));

        when(cacheManager.getCaches()).thenReturn(setOf(mapCache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        List<CacheOverlay> overlays = overlayManager.getOverlaysInZOrder();
        assertThat(overlays.size(), is(2));
        assertThat(overlays, hasItems(overlay1, overlay2));

        overlay2 = new CacheOverlayTest.TestCacheOverlay1(overlay2.getOverlayName(), overlay2.getCacheName(), overlay2.getCacheType());
        mapCache = new MapCache(mapCache.getName(), mapCache.getType(), mapCache.getSourceFile(), setOf(overlay2));
        Set<MapCache> updated = setOf(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this,
            Collections.<MapCache>emptySet(), updated, Collections.<MapCache>emptySet());

        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(mockOverlayOnMap(overlayManager));

        overlayManager.onCacheOverlaysUpdated(update);

        overlays = overlayManager.getOverlaysInZOrder();
        assertThat(overlays.size(), is(1));
        assertThat(overlays, hasItem(overlay2));
    }

    @Test
    public void addsOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("test overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));


        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("test overlay 2", "test cache", provider1.getClass());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay1, overlay2));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));
    }

    @Test
    public void addsAndRemovesOverlaysFromUpdatedCachesWhenOverlayCountIsUnchanged() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));

        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay 2", "test cache", provider1.getClass());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay2));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay2));
    }

    @Test
    public void replacesLikeOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), null, setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));

        CacheOverlay overlay1Updated = new CacheOverlayTest.TestCacheOverlay1(overlay1.getOverlayName(), overlay1.getCacheName(), overlay1.getCacheType());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay1Updated));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(
            this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());
        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));
        assertThat(overlayManager.getOverlaysInZOrder(), not(hasItem(sameInstance(overlay1))));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(sameInstance(overlay1Updated)));
    }

    @Test
    public void createsOverlaysOnMapLazily() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache mapCache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1));
        Set<MapCache> added = setOf(mapCache);
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, added, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet());
        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        overlayManager.onCacheOverlaysUpdated(update);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1));
        assertFalse(overlayManager.isOverlayVisible(overlay1));
        verify(provider1, never()).createOverlayOnMapFromCache(any(CacheOverlay.class), Mockito.same(overlayManager));

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(new TestOverlayOnMap(overlayManager));

        overlayManager.showOverlay(overlay1);

        assertTrue(overlayManager.isOverlayVisible(overlay1));
        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
    }


    @Test
    public void refreshesVisibleOverlayOnMapWhenUpdated() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));
        assertFalse(overlayManager.isOverlayVisible(overlay1));

        OverlayOnMapManager.OverlayOnMap onMap =  mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(onMap).addToMap();

        overlay1 = new CacheOverlayTest.TestCacheOverlay1(overlay1.getOverlayName(), overlay1.getCacheName(), overlay1.getCacheType());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay1));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());

        OverlayOnMapManager.OverlayOnMap onMapUpdated = mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMapUpdated);
        when(onMap.isOnMap()).thenReturn(true);
        when(onMap.isVisible()).thenReturn(true);

        overlayManager.onCacheOverlaysUpdated(update);

        verify(onMap).removeFromMap();
        verify(provider1).createOverlayOnMapFromCache(Mockito.same(overlay1), Mockito.same(overlayManager));
        verify(onMapUpdated).addToMap();
    }

    @Test
    public void doesNotRefreshHiddenOverlayOnMapWhenUpdated() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));
        assertFalse(overlayManager.isOverlayVisible(overlay1));

        OverlayOnMapManager.OverlayOnMap onMap =  mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(onMap).addToMap();

        overlay1 = new CacheOverlayTest.TestCacheOverlay1(overlay1.getOverlayName(), overlay1.getCacheName(), overlay1.getCacheType());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay1));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());

        OverlayOnMapManager.OverlayOnMap onMapUpdated = mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMapUpdated);
        when(onMap.isOnMap()).thenReturn(true);
        when(onMap.isVisible()).thenReturn(false);

        overlayManager.onCacheOverlaysUpdated(update);

        verify(onMap).removeFromMap();
        verify(provider1, never()).createOverlayOnMapFromCache(Mockito.same(overlay1), Mockito.same(overlayManager));
        verify(onMapUpdated, never()).addToMap();
    }

    @Test
    public void doesNotRefreshUnchangedVisibleOverlaysFromUpdatedCaches() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(1));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItem(overlay1));
        assertFalse(overlayManager.isOverlayVisible(overlay1));

        OverlayOnMapManager.OverlayOnMap onMap =  mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(onMap).addToMap();

        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay 2", cache.getName(), cache.getType());
        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay1, overlay2));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());

        OverlayOnMapManager.OverlayOnMap onMapUpdated = mockOverlayOnMap(overlayManager);

        overlayManager.onCacheOverlaysUpdated(update);

        verify(onMap, never()).removeFromMap();
        verify(provider1, times(1)).createOverlayOnMapFromCache(Mockito.same(overlay1), Mockito.same(overlayManager));
        verify(onMapUpdated, never()).addToMap();
    }

    @Test
    public void removesOverlayOnMapWhenOverlayIsRemovedFromCache() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay 2", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1, overlay2));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));

        OverlayOnMapManager.OverlayOnMap onMap =  mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(onMap).addToMap();

        cache = new MapCache(cache.getName(), cache.getType(), null, setOf(overlay2));
        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), setOf(cache), Collections.<MapCache>emptySet());

        overlayManager.onCacheOverlaysUpdated(update);

        verify(onMap).removeFromMap();
    }

    @Test
    public void removesOverlayOnMapWhenCacheIsRemoved() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay 2", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1, overlay2));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));

        OverlayOnMapManager.OverlayOnMap onMap =  mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(onMap).addToMap();

        CacheManager.CacheOverlayUpdate update = cacheManager.new CacheOverlayUpdate(this, Collections.<MapCache>emptySet(), Collections.<MapCache>emptySet(), setOf(cache));

        overlayManager.onCacheOverlaysUpdated(update);

        verify(onMap).removeFromMap();
    }

    @Test
    public void showsOverlayTheFirstTimeOverlayIsAdded() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay 1", "test cache", provider1.getClass());
        MapCache cache = new MapCache("test cache", provider1.getClass(), new File("test"), setOf(overlay1));

        when(cacheManager.getCaches()).thenReturn(setOf(cache));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertFalse(overlayManager.isOverlayVisible(overlay1));

        TestOverlayOnMap onMap =  new TestOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap);

        overlayManager.showOverlay(overlay1);

        assertTrue(overlayManager.isOverlayVisible(overlay1));
        assertTrue(onMap.isOnMap());
        assertTrue(onMap.isVisible());
    }

    @Test
    public void behavesWhenTwoCachesHaveOverlaysWithTheSameName() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay1", "cache1", provider1.getClass());
        MapCache cache1 = new MapCache("cache1", provider1.getClass(), null, setOf(overlay1));

        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay2("overlay1", "cache2", provider1.getClass());
        MapCache cache2 = new MapCache("cache2", provider1.getClass(), null, setOf(overlay2));

        when(cacheManager.getCaches()).thenReturn(setOf(cache1, cache2));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        assertThat(overlayManager.getOverlaysInZOrder().size(), is(2));
        assertThat(overlayManager.getOverlaysInZOrder(), hasItems(overlay1, overlay2));

        OverlayOnMapManager.OverlayOnMap onMap1 = mockOverlayOnMap(overlayManager);
        OverlayOnMapManager.OverlayOnMap onMap2 = mockOverlayOnMap(overlayManager);
        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap1);
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(onMap2);

        overlayManager.showOverlay(overlay1);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(provider1, never()).createOverlayOnMapFromCache(overlay2, overlayManager);
        verify(onMap1).addToMap();

        overlayManager.showOverlay(overlay2);

        verify(provider1).createOverlayOnMapFromCache(overlay1, overlayManager);
        verify(provider1).createOverlayOnMapFromCache(overlay2, overlayManager);
        verify(onMap1).addToMap();
        verify(onMap2).addToMap();

        when(onMap2.isVisible()).thenReturn(true);

        overlayManager.hideOverlay(overlay2);

        verify(onMap2).hide();
        verify(onMap1, never()).hide();
    }

    @Test
    public void behavesWhenTwoOverlaysAndTheirCachesHaveTheSameNames() {

        fail("unimplemented");
    }

    @Test
    public void maintainsOrderOfUpdatedCacheOverlays() {

        fail("unimplemented");
    }

    @Test
    public void forwardsMapClicksToOverlaysInZOrder() {

        fail("unimplemented");
    }

    @Test
    public void notifiesListenersWhenOverlaysUpdate() {

        fail("unimplemented");
    }

    @Test
    public void notifiesListenersWhenZOrderChanges() {

        fail("unimplemented");
    }

    @Test
    public void disposeStopsListeningToCacheManager() {

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
        overlayManager.dispose();

        verify(cacheManager).removeUpdateListener(overlayManager);
    }

    @Test
    public void disposeRemovesAndDisposesAllOverlays() {

        CacheOverlay overlay1 = new CacheOverlayTest.TestCacheOverlay1("overlay1", "cache1", provider1.getClass());
        CacheOverlay overlay2 = new CacheOverlayTest.TestCacheOverlay1("overlay2", "cache1", provider1.getClass());
        CacheOverlay overlay3 = new CacheOverlayTest.TestCacheOverlay2("overlay3", "cache2", provider2.getClass());
        MapCache cache1 = new MapCache("cache1", provider1.getClass(), null, setOf(overlay1, overlay2));
        MapCache cache2 = new MapCache("cache1", provider2.getClass(), null, setOf(overlay3));

        when(cacheManager.getCaches()).thenReturn(setOf(cache1, cache2));

        OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

        OverlayOnMapManager.OverlayOnMap onMap1 = mockOverlayOnMap(overlayManager);
        OverlayOnMapManager.OverlayOnMap onMap2 = mockOverlayOnMap(overlayManager);
        OverlayOnMapManager.OverlayOnMap onMap3 = mockOverlayOnMap(overlayManager);

        when(provider1.createOverlayOnMapFromCache(overlay1, overlayManager)).thenReturn(onMap1);
        when(provider1.createOverlayOnMapFromCache(overlay2, overlayManager)).thenReturn(onMap2);
        when(provider2.createOverlayOnMapFromCache(overlay3, overlayManager)).thenReturn(onMap3);

        overlayManager.showOverlay(overlay1);
        overlayManager.showOverlay(overlay2);
        overlayManager.showOverlay(overlay3);

        when(onMap1.isOnMap()).thenReturn(true);
        when(onMap2.isOnMap()).thenReturn(true);
        when(onMap3.isOnMap()).thenReturn(true);

        overlayManager.dispose();

        verify(onMap1).removeFromMap();
        verify(onMap1).dispose();
        verify(onMap2).removeFromMap();
        verify(onMap2).dispose();
        verify(onMap3).removeFromMap();
        verify(onMap3).dispose();
    }

    public class ZOrderTests {

        private CacheOverlay c1o1;
        private CacheOverlay c1o2;
        private CacheOverlay c1o3;
        private CacheOverlay c2o1;
        private CacheOverlay c2o2;
        private MapCache cache1;
        private MapCache cache2;

        @Before
        public void setup() {

            c1o1 = new CacheOverlayTest.TestCacheOverlay1("c1.1", "c1", provider1.getClass());
            c1o2 = new CacheOverlayTest.TestCacheOverlay1("c1.2", "c1", provider1.getClass());
            c1o3 = new CacheOverlayTest.TestCacheOverlay1("c1.3", "c1", provider1.getClass());
            cache1 = new MapCache("c1", provider1.getClass(), null, setOf(c1o1, c1o2, c1o3));

            c2o1 = new CacheOverlayTest.TestCacheOverlay2("c2.0", "c2", provider2.getClass());
            c2o2 = new CacheOverlayTest.TestCacheOverlay2("c2.1", "c2", provider2.getClass());
            cache2 = new MapCache("c2", provider2.getClass(), null, setOf(c2o2, c2o1));

            when(cacheManager.getCaches()).thenReturn(setOf(cache1, cache2));
        }

        @Test
        public void returnsModifiableCopyOfOverlayZOrder() {

            OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
            List<CacheOverlay> orderModified = overlayManager.getOverlaysInZOrder();
            Collections.reverse(orderModified);
            List<CacheOverlay> orderUnmodified = overlayManager.getOverlaysInZOrder();

            assertThat(orderUnmodified, not(sameInstance(orderModified)));
            assertThat(orderUnmodified, not(contains(orderModified.toArray())));
            assertThat(orderUnmodified.get(0), sameInstance(orderModified.get(orderModified.size() - 1)));
        }

        @Test
        public void initializesOverlaysOnMapWithProperZOrder() {

            OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
            List<CacheOverlay> order = overlayManager.getOverlaysInZOrder();
            int c1o1z = order.indexOf(c1o1);
            int c2o1z = order.indexOf(c2o1);

            TestOverlayOnMap c1o1OnMap = new TestOverlayOnMap(overlayManager);
            TestOverlayOnMap c2o1OnMap = new TestOverlayOnMap(overlayManager);
            when(provider1.createOverlayOnMapFromCache(c1o1, overlayManager)).thenReturn(c1o1OnMap);
            when(provider2.createOverlayOnMapFromCache(c2o1, overlayManager)).thenReturn(c2o1OnMap);

            overlayManager.showOverlay(c1o1);
            overlayManager.showOverlay(c2o1);

            assertThat(c1o1OnMap.getZIndex(), is(c1o1z));
            assertThat(c2o1OnMap.getZIndex(), is(c2o1z));
        }

        @Test
        public void setsComprehensiveZOrderFromList() {

            OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
            List<CacheOverlay> order = overlayManager.getOverlaysInZOrder();
            Collections.reverse(order);
            overlayManager.setZOrder(order);
            List<CacheOverlay> orderMod = overlayManager.getOverlaysInZOrder();

            assertThat(orderMod, equalTo(order));
        }

        @Test
        public void doesNotSetZOrderIfNewOrderHasDifferingElements() {

            OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);
            List<CacheOverlay> invalidOrder = overlayManager.getOverlaysInZOrder();
            invalidOrder.set(0, new CacheOverlayTest.TestCacheOverlay1("c1.1.tainted", "c1", provider1.getClass()));
            overlayManager.setZOrder(invalidOrder);

            List<CacheOverlay> unchangedOrder = overlayManager.getOverlaysInZOrder();

            assertThat(unchangedOrder, not(equalTo(invalidOrder)));
            assertThat(unchangedOrder, not(hasItem(invalidOrder.get(0))));
        }

        @Test
        public void movesTopZOrderToLowerZOrder() {

            final OverlayOnMapManager overlayManager = new OverlayOnMapManager(cacheManager, providers, null);

            when(provider1.createOverlayOnMapFromCache(any(CacheOverlay.class), same(overlayManager))).then(new Answer<OverlayOnMapManager.OverlayOnMap>() {
                @Override
                public OverlayOnMapManager.OverlayOnMap answer(InvocationOnMock invocation) throws Throwable {
                    OverlayOnMapManager.OverlayOnMap onMap = mockOverlayOnMap(overlayManager);
                    return onMap;
                }
            });
            overlayManager.changeZOrder(0, 1);

            assertThat(overlayManager.getOverlaysInZOrder().get(0).getOverlayName(), is("o1"));
            assertThat(overlayManager.getOverlaysInZOrder().get(1).getOverlayName(), is("o2"));

            fail("unimplemented");
        }

        @Test
        public void movesLowerToTop() {

        }
    }
}