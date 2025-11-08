const STATUS = document.getElementById('status');
const TBODY = document.querySelector('#data-table tbody');
const THRESHOLD = Number(document.getElementById('threshold').innerText || '75');
const FILTER = document.getElementById('filter');
const LIMIT = document.getElementById('limit');
const REFRESH = document.getElementById('refresh');
const COMM_TBODY = document.querySelector('#comms-table tbody');

function fmtTime(ts) {
	try { return new Date(ts).toLocaleString(); } catch (e) { return ts; }
}

function renderRows(data) {
	TBODY.innerHTML = '';
	for (const d of data) {
		const tr = document.createElement('tr');
		const isAlert = (typeof d.value === 'number') && d.value >= THRESHOLD;
		if (isAlert) tr.classList.add('alert');

		const tdDevice = document.createElement('td'); tdDevice.innerText = d.deviceId || '';
		const tdTime = document.createElement('td'); tdTime.innerText = fmtTime(d.timestamp);
		const tdVal = document.createElement('td'); tdVal.innerText = (d.value !== undefined) ? d.value : '';

		tr.appendChild(tdDevice);
		tr.appendChild(tdTime);
		tr.appendChild(tdVal);
		TBODY.appendChild(tr);
	}
}

async function loadData() {
	STATUS.innerText = 'Loading...';
	try {
		const res = await fetch('/api/data');
		if (!res.ok) throw new Error('HTTP ' + res.status);
		let data = await res.json();
		if (!Array.isArray(data) || data.length === 0) {
			TBODY.innerHTML = '<tr><td colspan="3">No data yet.</td></tr>';
			STATUS.innerText = 'No data available';
			return;
		}

		// sort by timestamp descending
		data.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

		// apply filter
		const f = (FILTER && FILTER.value) ? FILTER.value.trim().toLowerCase() : '';
		if (f) data = data.filter(d => (d.deviceId || '').toLowerCase().includes(f));

		const limit = Number(LIMIT.value) || 50;
		const sliced = data.slice(0, limit);

		renderRows(sliced);
		STATUS.innerText = `Last updated: ${new Date().toLocaleTimeString()} — showing ${sliced.length} of ${data.length}`;
	} catch (e) {
		TBODY.innerHTML = '<tr><td colspan="3">Failed to load data</td></tr>';
		STATUS.innerText = 'Failed to load data: ' + e.message;
	}
}

REFRESH.addEventListener('click', () => loadData());
FILTER.addEventListener('input', () => loadData());
LIMIT.addEventListener('change', () => loadData());

loadData();
setInterval(loadData, 3000);

async function loadComms() {
	if (!COMM_TBODY) return;
	try {
		const res = await fetch('/api/comms');
		if (!res.ok) throw new Error('HTTP ' + res.status);
		const data = await res.json();
		// data is array of { timestamp, direction, msg }
		COMM_TBODY.innerHTML = '';
		// show latest first
		for (let i = data.length - 1; i >= 0; i--) {
			const c = data[i];
			const tr = document.createElement('tr');
			const tdTime = document.createElement('td'); tdTime.innerText = c.timestamp ? new Date(c.timestamp).toLocaleString() : '';
			const tdDir = document.createElement('td'); tdDir.innerText = c.direction || '';
			const tdMsg = document.createElement('td'); tdMsg.innerText = c.msg || '';
			tr.appendChild(tdTime); tr.appendChild(tdDir); tr.appendChild(tdMsg);
			COMM_TBODY.appendChild(tr);
		}
	} catch (e) {
		// ignore errors for comms
	}
}

loadComms();
setInterval(loadComms, 3000);
