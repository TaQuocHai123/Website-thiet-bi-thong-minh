function openAddressMapPicker(initialLat, initialLng, callback) {
  // Ensure map container exists
  const existingModal = document.getElementById('addressMapModal');
  if (!existingModal) {
    const modalHtml = `
      <div class="modal fade" tabindex="-1" role="dialog" id="addressMapModal">
        <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">Chọn vị trí</h5>
              <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                <span aria-hidden="true">&times;</span>
              </button>
            </div>
            <div class="modal-body">
              <div id="addressMapPickerLoading" style="height: 400px; width: 100%; display:flex;align-items:center;justify-content:center;">
                <div style="text-align:center;color:#666"><div class="spinner-border" role="status"><span class="sr-only">Loading...</span></div><div style="margin-top:8px">Đang tải bản đồ...</div></div>
              </div>
              <div id="addressMapPicker" style="height: 400px; width: 100%; display:none"></div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy</button>
              <button type="button" id="addressMapSave" class="btn btn-primary">Lưu</button>
            </div>
          </div>
        </div>
      </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
  }

  // Initialize map when modal shown
  var map;
  var marker;
  function initPicker() {
    const latLng = { lat: initialLat || 10.8231, lng: initialLng || 106.6297 };
    map = new google.maps.Map(document.getElementById('addressMapPicker'), {
      center: latLng,
      zoom: 14,
      mapTypeControl: false
    });
    marker = new google.maps.Marker({
      position: latLng,
      map: map,
      draggable: true
    });
    map.addListener('click', function (e) {
      marker.setPosition(e.latLng);
    });
    // Hide loading placeholder and show map
    try { document.getElementById('addressMapPickerLoading').style.display = 'none'; } catch (e) { }
    try { document.getElementById('addressMapPicker').style.display = 'block'; } catch (e) { }
    // Trigger a resize once the map renders in the modal to avoid blank tiles
    setTimeout(function () {
      try {
        google.maps.event.trigger(map, 'resize');
        map.setCenter(latLng);
      } catch (err) {
        // ignore
      }
    }, 250);
  }

  // Leaflet init closure which assigns to the outer `marker` variable so save handler can access it
  function initPickerLeafletLocal(initialLat, initialLng) {
    const lat = initialLat || 10.8231;
    const lng = initialLng || 106.6297;
    try {
      document.getElementById('addressMapPickerLoading').style.display = 'none';
      document.getElementById('addressMapPicker').style.display = 'block';
      const mapEl = document.getElementById('addressMapPicker');
      // Clear any previous contents
      mapEl.innerHTML = '';
      const m = L.map(mapEl).setView([lat, lng], 14);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(m);
      marker = L.marker([lat, lng], { draggable: true }).addTo(m);
      m.on('click', function (e) {
        marker.setLatLng(e.latlng);
      });
      // Attach a getPosition interface similar to Google Maps marker for compatibility with save handler
      marker.getPosition = function () {
        const p = marker.getLatLng();
        return { toJSON: function () { return { lat: p.lat, lng: p.lng }; } };
      };
      map = m;
      console.debug('Leaflet picker initialized (local)');
    } catch (err) {
      console.warn('Leaflet init failed (local)', err);
      showMapLoadError();
    }
  }

  // Use jQuery modal to show
  // Register shown handler BEFORE showing modal so we don't miss the event
  $('#addressMapModal').off('shown.bs.modal').on('shown.bs.modal', function () {
    console.debug('Address map modal shown; waiting for Google Maps...');
    // Wait until the Google Maps API is available (it may be loading async)
    waitForGoogleMaps(1500).then(function () {
      try {
        console.debug('initPicker executed: from Google Maps');
        initPicker();
      } catch (e) {
        console.warn('Map init failed (Google) - falling back to Leaflet', e);
        // Try Leaflet fallback
        loadLeafletAndInit(initialLat, initialLng, initPickerLeafletLocal);
      }
    }).catch(function () {
      console.warn('Google Maps script not available — using Leaflet fallback');
      loadLeafletAndInit(initialLat, initialLng, initPickerLeafletLocal);
    });
  });
  $('#addressMapModal').modal('show');

  // Save handler
  $('#addressMapSave').off('click').on('click', function () {
    if (!marker || !marker.getPosition) {
      try { $('#addressMapModal').modal('hide'); } catch (e) { }
      if (callback) callback(null);
      return;
    }
    const pos = marker.getPosition().toJSON();
    try { $('#addressMapModal').modal('hide'); } catch (e) { }
    if (callback) callback(pos);
  });
}

// Wait for Google maps to become available; resolves if available before timeout
function waitForGoogleMaps(timeoutMs) {
  return new Promise(function (resolve, reject) {
    const start = Date.now();
    (function check() {
      if (window.google && window.google.maps) return resolve();
      if (Date.now() - start >= timeoutMs) return reject();
      setTimeout(check, 200);
    })();
  });
}

function showMapLoadError() {
  const container = document.getElementById('addressMapPicker');
  if (container) {
    container.innerHTML = '<div style="padding:24px;color:#333;background:#fff;border:1px solid #eee;border-radius:6px;">Không thể tải bản đồ. Vui lòng kiểm tra API key và mạng.</div>';
  }

  // Dynamically load Leaflet CSS/JS and initialize the map picker
  // callback receives (initialLat, initialLng) to initialize the picker
  function loadLeafletAndInit(initialLat, initialLng, callback) {
    const SPL = 'https://unpkg.com/leaflet@1.9.4/dist/';
    function loadCss(url) {
      return new Promise((res, rej) => {
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = url;
        link.onload = res; link.onerror = rej;
        document.head.appendChild(link);
      });
    }
    function loadScript(url) {
      return new Promise((res, rej) => {
        const s = document.createElement('script');
        s.src = url; s.async = true; s.defer = true;
        s.onload = res; s.onerror = rej;
        document.body.appendChild(s);
      });
    }

    (async function () {
      try {
        if (!(window.L && window.L.map)) {
          await loadCss(SPL + 'leaflet.css');
          await loadScript(SPL + 'leaflet.js');
        }
        if (typeof callback === 'function') callback(initialLat, initialLng); else initPickerLeaflet(initialLat, initialLng);
      } catch (e) {
        console.warn('Leaflet failed to load', e);
        showMapLoadError();
      }
    })();
  }

  // Default Leaflet initializer that can be overridden by callback passed above.
  function initPickerLeaflet(initialLat, initialLng) {
    const lat = initialLat || 10.8231;
    const lng = initialLng || 106.6297;
    try {
      document.getElementById('addressMapPickerLoading').style.display = 'none';
      document.getElementById('addressMapPicker').style.display = 'block';
      const mapEl = document.getElementById('addressMapPicker');
      // Clear any previous contents
      mapEl.innerHTML = '';
      const map = L.map(mapEl).setView([lat, lng], 14);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map);
      marker = L.marker([lat, lng], { draggable: true }).addTo(map);
      map.on('click', function (e) {
        marker.setLatLng(e.latlng);
      });
      // Attach a getPosition interface similar to Google Maps marker for compatibility with save handler
      marker.getPosition = function () {
        const p = marker.getLatLng();
        return { toJSON: function () { return { lat: p.lat, lng: p.lng }; } };
      };
      console.debug('Leaflet picker initialized');
    } catch (err) {
      console.warn('Leaflet init failed', err);
      showMapLoadError();
    }
  }
}


// Expose as global for inline usage
window.openAddressMapPicker = openAddressMapPicker;
