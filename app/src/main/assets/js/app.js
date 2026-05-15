/* ===================================
   猫药管家 — 应用逻辑
   ⚠️ 这部分的代码建议不要修改
   如果改界面样式，请编辑 style.css
   如果改页面结构，请编辑 index.html
   =================================== */

// ===== 全局状态 =====
const STATE = {
    currentPage: 'home',
    calYear: 0,
    calMonth: 0,
    selectedDay: null,
    medications: [],
    records: [],
    exercises: []
};

// ===== 工具函数 =====
function toDateStr(ts) {
    const d = new Date(ts);
    return d.getFullYear() + '-' + String(d.getMonth()+1).padStart(2,'0') + '-' + String(d.getDate()).padStart(2,'0');
}
function toTimeStr(ts) {
    const d = new Date(ts);
    return String(d.getHours()).padStart(2,'0') + ':' + String(d.getMinutes()).padStart(2,'0');
}
function toDisplayDate(ts) {
    const d = new Date(ts);
    return d.getFullYear() + '年' + (d.getMonth()+1) + '月' + d.getDate() + '日';
}
function getDayStart(ts) {
    const d = new Date(ts);
    d.setHours(0,0,0,0);
    return d.getTime();
}
function getDayEnd(ts) {
    const d = new Date(ts);
    d.setHours(23,59,59,999);
    return d.getTime();
}

// ===== 导航 =====
document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => {
        const page = btn.dataset.page;
        switchPage(page);
    });
});
function switchPage(page) {
    STATE.currentPage = page;
    document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.page === page));
    document.querySelectorAll('.page').forEach(p => p.classList.toggle('active', p.id === 'page-' + page));
}

// ===== 弹窗 =====
function showModal() { document.getElementById('medModal').style.display = 'flex'; }
function closeModal() {
    document.getElementById('medModal').style.display = 'none';
    document.getElementById('medName').value = '';
    document.getElementById('medType').value = 'drug';
    document.getElementById('medStrength').value = '';
}
document.getElementById('medType').addEventListener('change', function() {
    document.getElementById('strengthGroup').style.display = this.value === 'drug' ? 'block' : 'none';
});

// ===== 首页 =====
async function refreshHome() {
    const meds = JSON.parse(await Android.getMedications());
    const now = new Date();
    const start = getDayStart(now.getTime());
    const end = getDayEnd(now.getTime());

    // 加载今天的喂药记录
    const recordsRaw = JSON.parse(await Android.getMedicationRecords(now.getFullYear(), now.getMonth()+1));
    const todayRecords = recordsRaw.filter(r => r.actualTime >= start && r.actualTime <= end);

    // 下次喂药
    const nextEl = document.getElementById('nextReminder');
    const todayMeds = document.getElementById('todayMeds');

    if (meds.length === 0) {
        nextEl.textContent = '请先在「设置」中添加药品';
        todayMeds.innerHTML = '<div class="empty-state">暂无药品</div>';
        return;
    }

    // 计算下次喂药时间
    let nextTime = null;
    let nextName = '';
    for (const m of meds) {
        const slots = [
            { h: m.reminderHour, m: m.reminderMinute },
            { h: m.reminderHour2, m: m.reminderMinute2 }
        ];
        for (const s of slots) {
            const t = new Date();
            t.setHours(s.h, s.m, 0, 0);
            if (t.getTime() > now.getTime()) {
                if (!nextTime || t.getTime() < nextTime) {
                    nextTime = t.getTime();
                    nextName = m.name;
                }
            }
        }
    }
    if (nextTime) {
        nextEl.innerHTML = '<span class="next-med-name">' + nextName + '</span> 在 ' + toTimeStr(nextTime);
    } else {
        nextEl.textContent = '今日提醒已全部完成 ✅';
    }

    // 今日用药列表
    let html = '';
    for (const m of meds) {
        const medRecords = todayRecords.filter(r => r.medicationId === m.id);
        const taken = medRecords.filter(r => r.status === 'taken');
        const missed = medRecords.filter(r => r.status === 'missed');
        const slot1Taken = taken.some(r => {
            const h = new Date(r.actualTime).getHours();
            return h < 14;
        });
        const slot2Taken = taken.some(r => {
            const h = new Date(r.actualTime).getHours();
            return h >= 14;
        });
        const s1Time = String(m.reminderHour).padStart(2,'0') + ':' + String(m.reminderMinute).padStart(2,'0');
        const s2Time = String(m.reminderHour2).padStart(2,'0') + ':' + String(m.reminderMinute2).padStart(2,'0');
        html += '<div class="today-item">';
        html += '<div><strong>' + m.name + '</strong><br><span class="time">' + s1Time + ' / ' + s2Time + '</span></div>';
        html += '<div>';
        html += slot1Taken ? '<span class="status-taken">✔ 已喂</span>' : '<span class="status-missed">—</span>';
        html += ' ';
        html += slot2Taken ? '<span class="status-taken">✔ 已喂</span>' : '<span class="status-missed">—</span>';
        html += '</div></div>';
    }
    todayMeds.innerHTML = html;

    // 运动状态
    const exRecords = JSON.parse(await Android.getExerciseRecords(now.getFullYear(), now.getMonth()+1));
    const todayEx = exRecords.filter(r => r.date >= start && r.date <= end);
    if (todayEx.length > 0) {
        document.getElementById('todayExercise').innerHTML = todayEx.map(r =>
            '<div class="today-item"><span>🏃 运动 ' + r.durationMinutes + ' 分钟</span><span class="status-taken">✔</span></div>'
        ).join('');
    } else {
        document.getElementById('todayExercise').innerHTML = '<div class="empty-state">今天还没有运动记录</div>';
    }
}

// 记录运动
function recordExercise() {
    const now = new Date();
    const data = JSON.stringify({
        date: now.getTime(),
        durationMinutes: 30,
        note: ''
    });
    Android.saveExerciseRecord(data);
    refreshHome();
}

// ===== 计算器 =====
function doCalc() {
    const w = parseFloat(document.getElementById('calcWeight').value);
    const d = parseFloat(document.getElementById('calcDosage').value);
    const s = parseFloat(document.getElementById('calcStrength').value);
    if (!w || !d || !s) { alert('请填写所有字段'); return; }

    const result = JSON.parse(Android.calculateDosage(w, d, s));
    const el = document.getElementById('calcResult');
    el.style.display = 'block';

    let fractionStr = '';
    if (result.fractionDenominator > 1) {
        fractionStr = '（约 ' + result.fractionNumerator + '/' + result.fractionDenominator + ' 片）';
    }

    el.innerHTML = `
        <div class="result-main">${result.pills} 片 ${fractionStr}</div>
        <div class="result-sub">
            需要 ${result.requiredMg} mg<br>
            体重 ${result.weightKg} kg × 剂量 ${result.mgPerKg} mg/kg = ${result.requiredMg} mg<br>
            ${result.requiredMg} mg ÷ ${result.strengthMg} mg/片 = ${result.pills} 片
        </div>
    `;
}
// 快速填入药片规格
function setStrength(val) {
    document.getElementById('calcStrength').value = val;
}

// ===== 日历 =====
function initCalendar() {
    const now = new Date();
    STATE.calYear = now.getFullYear();
    STATE.calMonth = now.getMonth() + 1;
    renderCalendar();
}
function changeMonth(delta) {
    STATE.calMonth += delta;
    if (STATE.calMonth > 12) { STATE.calMonth = 1; STATE.calYear++; }
    if (STATE.calMonth < 1) { STATE.calMonth = 12; STATE.calYear--; }
    renderCalendar();
}
async function renderCalendar() {
    const y = STATE.calYear, m = STATE.calMonth;
    document.getElementById('calMonthYear').textContent = y + '年' + m + '月';

    // 加载数据
    STATE.records = JSON.parse(await Android.getMedicationRecords(y, m));
    STATE.exercises = JSON.parse(await Android.getExerciseRecords(y, m));

    const firstDay = new Date(y, m - 1, 1).getDay();
    const daysInMonth = new Date(y, m, 0).getDate();
    const today = new Date();
    const todayStr = toDateStr(today.getTime());

    let html = '';
    // 上月填充
    const prevMonthDays = new Date(y, m - 1, 0).getDate();
    for (let i = firstDay - 1; i >= 0; i--) {
        html += '<div class="cal-day other-month">' + (prevMonthDays - i) + '</div>';
    }
    // 当月
    for (let d = 1; d <= daysInMonth; d++) {
        const dateObj = new Date(y, m - 1, d);
        const dateStr = toDateStr(dateObj.getTime());
        const isToday = dateStr === todayStr;
        const isSelected = STATE.selectedDay === dateStr;
        const dayStart = getDayStart(dateObj.getTime());
        const dayEnd = getDayEnd(dateObj.getTime());

        const hasMed = STATE.records.some(r => r.actualTime >= dayStart && r.actualTime <= dayEnd && r.status === 'taken');
        const hasEx = STATE.exercises.some(r => r.date >= dayStart && r.date <= dayEnd);

        let cls = 'cal-day';
        if (isToday) cls += ' today';
        if (isSelected) cls += ' selected';

        let dots = '';
        if (hasMed || hasEx) {
            dots = '<div class="dots">';
            if (hasMed) dots += '<span class="dot med"></span>';
            if (hasEx) dots += '<span class="dot exercise"></span>';
            dots += '</div>';
        }

        html += '<div class="' + cls + '" data-date="' + dateStr + '" onclick="selectDay(\'' + dateStr + '\')">' + d + dots + '</div>';
    }
    // 下月填充
    const totalCells = firstDay + daysInMonth;
    const remaining = (7 - totalCells % 7) % 7;
    for (let i = 1; i <= remaining; i++) {
        html += '<div class="cal-day other-month">' + i + '</div>';
    }
    document.getElementById('calGrid').innerHTML = html;

    // 显示选中日详情
    if (STATE.selectedDay && STATE.selectedDay.startsWith(y + '-' + String(m).padStart(2,'0'))) {
        showDayDetail(STATE.selectedDay);
    } else {
        STATE.selectedDay = todayStr;
        showDayDetail(todayStr);
    }
}
function selectDay(dateStr) {
    STATE.selectedDay = dateStr;
    renderCalendar();
}
function showDayDetail(dateStr) {
    const parts = dateStr.split('-');
    const dayStart = getDayStart(new Date(parseInt(parts[0]), parseInt(parts[1])-1, parseInt(parts[2])).getTime());
    const dayEnd = getDayEnd(dayStart);
    const el = document.getElementById('dayDetail');

    const dayRecords = STATE.records.filter(r => r.actualTime >= dayStart && r.actualTime <= dayEnd);
    const dayExercises = STATE.exercises.filter(r => r.date >= dayStart && r.date <= dayEnd);

    if (dayRecords.length === 0 && dayExercises.length === 0) {
        el.innerHTML = '<div class="empty-state">' + toDisplayDate(dayStart) + ' 无记录</div>';
        return;
    }

    let html = '<div style="font-weight:600;margin-bottom:8px">' + toDisplayDate(dayStart) + '</div>';

    for (const r of dayRecords) {
        const statusText = r.status === 'taken' ? '✅ 已喂' : (r.status === 'missed' ? '❌ 漏服' : '⏰ 推迟');
        const dosageText = r.dosageMg ? ' | ' + r.dosageMg + 'mg' : '';
        html += '<div class="day-detail-item">💊 ' + toTimeStr(r.actualTime) + ' ' + statusText + dosageText + '</div>';
    }
    for (const r of dayExercises) {
        html += '<div class="day-detail-item">🏃 运动 ' + r.durationMinutes + ' 分钟' + (r.note ? ' | ' + r.note : '') + '</div>';
    }
    el.innerHTML = html;
}

// ===== 设置 - 药品管理 =====
async function refreshSettings() {
    const meds = JSON.parse(await Android.getMedications());
    const el = document.getElementById('medList');

    if (meds.length === 0) {
        el.innerHTML = '<div class="empty-state">暂未添加药品或补剂</div>';
    } else {
        el.innerHTML = meds.map(m => {
            const type = m.type === 'drug' ? '💊' : '🧪';
            const strength = m.strengthMg ? m.strengthMg + 'mg/片' : '—';
            return '<div class="settings-item">' +
                '<div><span class="med-name">' + type + ' ' + m.name + '</span><br><span class="med-info">' + strength + '</span></div>' +
                '<button class="btn btn-outline" style="padding:4px 12px;font-size:12px" onclick="deleteMed(' + m.id + ')">删除</button>' +
                '</div>';
        }).join('');
    }

    // 体重
    const weight = JSON.parse(await Android.getLatestWeight());
    if (weight && weight.weightKg) {
        document.getElementById('currentWeight').textContent = weight.weightKg + ' kg（' + toDisplayDate(weight.date) + '）';
    }

    // 权限
    const hasOverlay = Android.hasOverlayPermission();
    const hasAlarm = Android.hasExactAlarmPermission();
    document.getElementById('permStatus').innerHTML =
        '悬浮窗权限：' + (hasOverlay ? '✅ 已授权' : '❌ 未授权') + '<br>' +
        '精确闹钟权限：' + (hasAlarm ? '✅ 已授权' : '❌ 未授权');
}

function showAddMed() {
    showModal();
    document.getElementById('medStrength').value = '';
    document.getElementById('strengthGroup').style.display = 'block';
}

function saveMed() {
    const name = document.getElementById('medName').value.trim();
    const type = document.getElementById('medType').value;
    const strength = parseFloat(document.getElementById('medStrength').value);
    if (!name) { alert('请输入名称'); return; }

    const data = {
        name: name,
        type: type,
        strengthMg: type === 'drug' && strength ? strength : null,
        reminderHour: 8, reminderMinute: 30,
        reminderHour2: 20, reminderMinute2: 30
    };
    Android.saveMedication(JSON.stringify(data));
    closeModal();
    setTimeout(() => { refreshHome(); refreshSettings(); }, 300);
}

function deleteMed(id) {
    if (!confirm('确定删除此药品？')) return;
    Android.deleteMedication(id);
    setTimeout(() => { refreshHome(); refreshSettings(); }, 300);
}

// 保存体重
function saveWeight() {
    const w = parseFloat(document.getElementById('newWeight').value);
    if (!w) { alert('请输入体重'); return; }
    Android.saveWeight(JSON.stringify({ weightKg: w, date: Date.now() }));
    document.getElementById('newWeight').value = '';
    refreshSettings();
}

// 权限
function requestPerms() {
    if (!Android.hasOverlayPermission()) Android.openOverlaySettings();
}

// ===== 刷新数据 =====
async function refreshData() {
    await refreshHome();
    if (STATE.currentPage === 'settings') await refreshSettings();
}

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', async function() {
    // 更新日期头
    document.getElementById('headerDate').textContent = toDisplayDate(Date.now());

    // 检查是否有预设药品，填入快速选择
    const meds = JSON.parse(await Android.getMedications());
    const drugs = meds.filter(m => m.type === 'drug' && m.strengthMg);
    if (drugs.length > 0) {
        const container = document.getElementById('quickStrength');
        container.innerHTML = drugs.map(m =>
            '<button onclick="setStrength(' + m.strengthMg + ')">' + m.name + ' ' + m.strengthMg + 'mg</button>'
        ).join('');
    }

    initCalendar();
    await refreshHome();
    await refreshSettings();
});
