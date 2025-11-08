const STATUS = document.getElementById('status');
const TBODY = document.querySelector('#data-table tbody');
const THRESHOLD = Number(document.getElementById('threshold').innerText || '75');

function fmtTime(ts) {
	try { return new Date(ts).toLocaleString(); } catch (e) { return ts; }
}

async function loadData() {
	STATUS.innerText = 'Loading data...';
	try {
		const res = await fetch('/api/data');
		if (!res.ok) throw new Error('HTTP ' + res.status);
		const data = await res.json();
		if (!Array.isArray(data) || data.length === 0) {
			TBODY.innerHTML = '<tr><td colspan="3">No data yet.</td></tr>';
			STATUS.innerText = 'No data available';
			return;
		}

		// sort by timestamp descending
		data.sort((a,b) => (b.timestamp || 0) - (a.timestamp || 0));

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

		STATUS.innerText = `Last updated: ${new Date().toLocaleTimeString()} — ${data.length} readings`;
	} catch (e) {
		TBODY.innerHTML = '<tr><td colspan="3">Failed to load data</td></tr>';
		STATUS.innerText = 'Failed to load data: ' + e.message;
	}
}

loadData();
setInterval(loadData, 3000);
