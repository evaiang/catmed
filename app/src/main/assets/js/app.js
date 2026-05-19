/* =============================
   猫药管家 v2 — 应用逻辑
   ============================= */

// ===== 工具 =====
function todayStr() {
    const d = new Date();
    return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
}
function tsToStr(ts) {
    const d = new Date(ts);
    return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0');
}
function dateKey(y,m,d) { return y+'-'+String(m).padStart(2,'0')+'-'+String(d).padStart(2,'0'); }

// ===== 全局状态 =====
let eventTypes = [];
let eventRecords = [];
let calYear = 0, calMonth = 0;

// ===== 数据加载（异步） =====
async function loadTypes() {
    try {
        eventTypes = JSON.parse(await Android.getEventTypes());
    } catch(e) { eventTypes = []; }
}
async function loadRecords(year, month) {
    try {
        eventRecords = JSON.parse(await Android.getEventRecords(year, month));
    } catch(e) { eventRecords = []; }
}

// ===== 导航 =====
document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => {
        const tab = btn.dataset.tab;
        document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
        document.querySelectorAll('.page').forEach(p => p.classList.toggle('active', p.id === 'page-' + tab));
        if (tab === 'home') refreshHome();
        if (tab === 'cal') refreshCalendar();
        if (tab === 'settings') refreshSettings();
    });
});

// ===== 首页 =====
async function refreshHome() {
    await loadTypes();
    const now = new Date();
    await loadRecords(now.getFullYear(), now.getMonth() + 1);

    const today = todayStr();
    const todayRecords = eventRecords.filter(r => tsToStr(r.time) === today && !r.undone);

    const el = document.getElementById('eventList');
    if (eventTypes.length === 0) {
        el.innerHTML = '<div class="empty-events">暂无事件，去「设置」添加</div>';
        document.getElementById('todaySummary').textContent = '';
        return;
    }

    let totalDone = 0, totalTarget = 0;
    let html = '';
    for (const t of eventTypes) {
        const count = todayRecords.filter(r => r.typeId === t.id).length;
        totalDone += count; totalTarget += t.dailyTarget;

        const slots = [];
        for (let i = 0; i < t.dailyTarget; i++) {
            const done = i < count;
            slots.push(`<button class="event-record-btn ${done?'done':'pending'}"
                onclick="${done ? 'undoRecord('+t.id+')' : 'addRecord('+t.id+')'}"
                title="${done?'撤销':'我做了！'}">${done?'✓':''}</button>`);
        }

        const pct = Math.min(100, Math.round(count / t.dailyTarget * 100));
        html += `
            <div class="event-row">
                <div class="event-icon" style="background:${t.color}20;color:${t.color}"><span class="m-icon lg">${t.icon}</span></div>
                <div class="event-info">
                    <div class="event-name">${t.name}</div>
                    <div class="event-progress-text">${count > 0 ? '已记录 ' + count + ' 次' : '今天还没记录'}</div>
                    <div class="event-bar-wrap"><div class="event-bar-fill" style="width:${pct}%;background:${t.color}"></div></div>
                </div>
                <div class="event-actions">${slots.join('')}</div>
            </div>`;
    }
    el.innerHTML = html;
    document.getElementById('todaySummary').textContent =
        totalDone > 0 ? '今日已记录 ' + totalDone + ' 次' : '今天还没开始记录';
}

function addRecord(typeId) {
    Android.saveEventRecord(JSON.stringify({ typeId, time: Date.now(), undone: false }));
    refreshHome();
    if (document.querySelector('.page#page-cal.active')) refreshCalendar();
}

function undoRecord(typeId) {
    const today = todayStr();
    const found = eventRecords.filter(r => r.typeId === typeId && tsToStr(r.time) === today && !r.undone);
    if (found.length > 0) {
        Android.deleteEventRecord(found[found.length - 1].id);
    }
    refreshHome();
    if (document.querySelector('.page#page-cal.active')) refreshCalendar();
}

// ===== 计算器 =====
function doCalc() {
    const w = parseFloat(document.getElementById('calcWeight').value);
    const d = parseFloat(document.getElementById('calcDosage').value);
    const s = parseFloat(document.getElementById('calcStrength').value);
    if (!w || !d || !s) return;

    const needMg = w * d;
    const pills = needMg / s;

    function gcd(a,b) { return b?gcd(b,a%b):a; }
    const denom = [1,2,3,4,5,6,8].reduce((best,x) =>
        Math.abs(pills - Math.round(pills*x)/x) < Math.abs(pills - Math.round(pills*best)/best) ? x : best, 1);
    const num = Math.round(pills * denom);
    const g = gcd(num, denom);
    const fNum = num/g, fDen = denom/g;

    const el = document.getElementById('calcResult');
    el.style.display = 'block';
    document.getElementById('calcMainResult').innerHTML =
        '<span class="big-num">' + needMg.toFixed(1) + '</span><span class="big-unit">mg / 次</span>';
    document.getElementById('calcDetail').innerHTML =
        '等于 <span>' + pills.toFixed(2) + '</span> 片' +
        (fDen > 1 ? '（约 <span>' + fNum + '/' + fDen + '</span> 片）' : '') +
        '<br>计算过程：' + w + 'kg × ' + d + 'mg/kg = ' + needMg.toFixed(1) + 'mg<br>' +
        needMg.toFixed(1) + 'mg ÷ ' + s + 'mg/片 = ' + pills.toFixed(2) + '片';
}

// ===== 日历 =====
function initCal() {
    const now = new Date();
    calYear = now.getFullYear();
    calMonth = now.getMonth() + 1;
}
function changeMonth(d) {
    calMonth += d;
    if (calMonth > 12) { calMonth = 1; calYear++; }
    if (calMonth < 1) { calMonth = 12; calYear--; }
    refreshCalendar();
}

async function refreshCalendar() {
    await loadTypes();
    await loadRecords(calYear, calMonth);

    document.getElementById('calLabel').textContent = calYear + '年' + calMonth + '月';
    const firstDay = new Date(calYear, calMonth-1, 1).getDay();
    const daysInMonth = new Date(calYear, calMonth, 0).getDate();
    const prevDays = new Date(calYear, calMonth-1, 0).getDate();
    const today = todayStr();

    const byDay = {};
    for (const r of eventRecords) {
        if (r.undone) continue;
        const ds = tsToStr(r.time);
        if (!byDay[ds]) byDay[ds] = {};
        if (!byDay[ds][r.typeId]) byDay[ds][r.typeId] = 0;
        byDay[ds][r.typeId]++;
    }

    let html = '';
    for (let i = firstDay-1; i >= 0; i--)
        html += '<div class="cal-day other">' + (prevDays - i) + '</div>';

    for (let d = 1; d <= daysInMonth; d++) {
        const ds = dateKey(calYear, calMonth, d);
        const isToday = ds === today;
        const dayData = byDay[ds] || {};
        let cls = 'cal-day';
        if (isToday) cls += ' today';

        let dots = '';
        for (const t of eventTypes) {
            const count = dayData[t.id] || 0;
            const pct = count / t.dailyTarget;
            if (pct >= 1)
                dots += '<span class="cal-dot" style="background:' + t.color + '"></span>';
            else if (pct > 0)
                dots += '<span class="cal-dot" style="background:' + t.color + ';opacity:0.4"></span>';
        }

        html += '<div class="' + cls + '" data-ds="' + ds + '" onclick="pickDay(\'' + ds + '\')">'
            + d + '<div class="cal-dots">' + dots + '</div></div>';
    }

    const total = firstDay + daysInMonth;
    const rem = (7 - total % 7) % 7;
    for (let i = 1; i <= rem; i++)
        html += '<div class="cal-day other">' + i + '</div>';

    document.getElementById('calGrid').innerHTML = html;

    document.getElementById('calLegend').innerHTML = eventTypes.map(t =>
        '<div class="cal-legend-item"><span class="cal-legend-dot" style="background:' + t.color + '"></span>' + t.name + '</div>'
    ).join('');

    refreshMonthStats();
}

function pickDay(ds) {
    document.querySelectorAll('.cal-day.selected').forEach(x => x.classList.remove('selected'));
    const el = document.querySelector('.cal-day[data-ds="' + ds + '"]');
    if (el) el.classList.add('selected');

    const dayData = {};
    for (const r of eventRecords) {
        if (tsToStr(r.time) === ds && !r.undone) {
            if (!dayData[r.typeId]) dayData[r.typeId] = 0;
            dayData[r.typeId]++;
        }
    }
    const parts = ds.split('-');
    const label = parts[0] + '年' + parseInt(parts[1]) + '月' + parseInt(parts[2]) + '日';

    let html = '<div style="font-weight:600;margin-bottom:6px">' + label + '</div>';
    let hasAny = false;
    for (const t of eventTypes) {
        const c = dayData[t.id] || 0;
        if (c > 0) hasAny = true;
        const desc = c > 0
            ? '<span style="color:var(--c-success);font-weight:500">已记录 ' + c + ' 次</span>'
            : '<span style="color:var(--c-text-secondary)">未记录</span>';
        html += '<div><span class="m-icon" style="font-size:16px;color:' + t.color + '">' + t.icon + '</span> ' + t.name + '：' + desc + '</div>';
    }
    if (!hasAny) html += '<div class="no-record">这天还没有记录</div>';
    document.getElementById('daySummary').innerHTML = html;
}

function refreshMonthStats() {
    const start = new Date(calYear, calMonth-1, 1).getTime();
    const end = new Date(calYear, calMonth, 0, 23, 59, 59).getTime();

    const stats = {};
    const daysInMonth = new Date(calYear, calMonth, 0).getDate();
    for (const r of eventRecords) {
        if (r.time >= start && r.time <= end && !r.undone) {
            if (!stats[r.typeId]) stats[r.typeId] = 0;
            stats[r.typeId]++;
        }
    }

    let html = '';
    for (const t of eventTypes) {
        const count = stats[t.id] || 0;
        const target = t.dailyTarget * daysInMonth;
        const pct = target > 0 ? Math.round(count / target * 100) : 0;
        const bar = '█'.repeat(Math.round(pct/10)) + '░'.repeat(10 - Math.round(pct/10));
        html += '<div style="margin-bottom:8px">' +
            '<span><span class="m-icon" style="font-size:16px;color:' + t.color + '">' + t.icon + '</span> <strong>' + t.name + '</strong></span>' +
            '<span style="float:right;color:var(--c-text-secondary)">' + count + ' / ' + target + ' 次</span>' +
            '<div style="font-size:10px;color:#CCC;letter-spacing:2px;margin-top:2px">' + bar + ' ' + pct + '%</div>' +
            '</div>';
    }
    document.getElementById('monthStats').innerHTML = html;
}

// ===== 设置 =====
async function refreshSettings() {
    await loadTypes();
    const el = document.getElementById('typeList');
    if (eventTypes.length === 0) {
        el.innerHTML = '<div class="empty-events">暂无事件类型，点上方按钮添加</div>';
        return;
    }
    el.innerHTML = eventTypes.map(t => {
        let reminders = [];
        try { reminders = JSON.parse(t.reminders); } catch(e) {}
        const remStr = t.remindOn && reminders.length > 0
            ? '提醒 ' + reminders.join('、')
            : '不提醒';
        return `<div class="event-type-item">
            <div class="event-type-icon" style="background:${t.color}20;color:${t.color}"><span class="m-icon lg">${t.icon}</span></div>
            <div class="event-type-info">
                <div class="event-type-name">${t.name}</div>
                <div class="event-type-meta">每日 ${t.dailyTarget} 次 · ${remStr}</div>
            </div>
            <button class="btn btn-sm btn-outline" onclick="editType(${t.id})" style="margin-right:4px">编辑</button>
            <button class="delete-btn" onclick="deleteType(${t.id})">删除</button>
        </div>`;
    }).join('');
}

// ===== 弹窗（事件类型编辑） =====
let remindOn = true;
let remindTimes = ['08:00'];

function showAddType() {
    document.getElementById('typeModalTitle').textContent = '添加事件类型';
    document.getElementById('editTypeId').value = '';
    document.getElementById('typeName').value = '';
    document.getElementById('typeIcon').value = 'pill';
    document.getElementById('typeColor').value = '#0891B2';
    document.getElementById('typeDaily').value = 2;
    remindOn = true;
    document.getElementById('typeRemindToggle').className = 'event-type-toggle on';
    document.getElementById('remindTimesField').style.display = 'block';
    remindTimes = ['08:00'];
    renderRemindPills();
    document.getElementById('typeModal').style.display = 'flex';
}

function editType(id) {
    const t = eventTypes.find(x => x.id === id);
    if (!t) return;
    document.getElementById('typeModalTitle').textContent = '编辑事件类型';
    document.getElementById('editTypeId').value = id;
    document.getElementById('typeName').value = t.name;
    document.getElementById('typeIcon').value = t.icon;
    document.getElementById('typeColor').value = t.color;
    document.getElementById('typeDaily').value = t.dailyTarget;
    remindOn = t.remindOn;
    document.getElementById('typeRemindToggle').className = 'event-type-toggle' + (remindOn ? ' on' : '');
    document.getElementById('remindTimesField').style.display = remindOn ? 'block' : 'none';
    try { remindTimes = JSON.parse(t.reminders); } catch(e) { remindTimes = ['08:00']; }
    if (remindTimes.length === 0) remindTimes = ['08:00'];
    renderRemindPills();
    document.getElementById('typeModal').style.display = 'flex';
}

function closeTypeModal() {
    document.getElementById('typeModal').style.display = 'none';
}

function toggleRemind() {
    remindOn = !remindOn;
    document.getElementById('typeRemindToggle').className = 'event-type-toggle' + (remindOn ? ' on' : '');
    document.getElementById('remindTimesField').style.display = remindOn ? 'block' : 'none';
}

function addRemindTime() {
    if (remindTimes.length >= 8) return;
    remindTimes.push('12:00');
    renderRemindPills();
}

function removeRemindTime(i) { remindTimes.splice(i, 1); renderRemindPills(); }
function updateRemindTime(i, val) { remindTimes[i] = val; }

function renderRemindPills() {
    const el = document.getElementById('remindPills');
    el.innerHTML = remindTimes.map((t, i) =>
        '<span class="time-pill selected">' +
        '<input type="time" value="' + t + '" onchange="updateRemindTime(' + i + ', this.value)" style="border:none;background:none;font-size:12px;width:auto;padding:0">' +
        (remindTimes.length > 1 ? '<span class="remove" onclick="removeRemindTime(' + i + ')">×</span>' : '') +
        '</span>'
    ).join('');
}

async function saveType() {
    const name = document.getElementById('typeName').value.trim();
    if (!name) return;
    const editId = document.getElementById('editTypeId').value;

    const obj = {
        id: editId ? parseInt(editId) : 0,
        name,
        icon: document.getElementById('typeIcon').value,
        color: document.getElementById('typeColor').value,
        dailyTarget: parseInt(document.getElementById('typeDaily').value) || 1,
        reminders: JSON.stringify(remindOn ? remindTimes : []),
        remindOn
    };

    Android.saveEventType(JSON.stringify(obj));
    closeTypeModal();
    await loadTypes();
    refreshHome(); refreshCalendar(); refreshSettings();
}

async function deleteType(id) {
    if (!confirm('确定删除此事件类型？')) return;
    Android.deleteEventType(id);
    await loadTypes();
    refreshHome(); refreshCalendar(); refreshSettings();
}

// ===== 快速选择药片规格 =====
function setupQuickPills() {
    let btns = '<button onclick="document.getElementById(\'calcStrength\').value=\'30\'">30mg</button>' +
               '<button onclick="document.getElementById(\'calcStrength\').value=\'50\'">50mg</button>';
    document.getElementById('quickPills').innerHTML = btns;
}

// ===== 权限 =====
function requestPerms() {
    if (!Android.hasOverlayPermission()) Android.openOverlaySettings();
}

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', async function() {
    document.getElementById('headerDate').textContent =
        new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'short' });

    setupQuickPills();
    initCal();
    await loadTypes();
    refreshHome();
    refreshCalendar();
    refreshSettings();
});
