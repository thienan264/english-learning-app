/* January uses actual December revenue, and future months are omitted. */
globalThis.RevenueTrend = {
    calculate(revenue, december, currentMonth) {
        return revenue.map((value, index) => {
            if (index >= currentMonth) return null;
            const previous = Number(index === 0 ? december : revenue[index - 1]);
            const delta = Number(value) - previous;
            return {delta, percent: previous > 0 ? delta / previous * 100 : delta === 0 ? 0 : null};
        });
    },
    describe(change, money) {
        if (!change) return 'Chưa có dữ liệu';
        if (change.delta === 0) return 'Không đổi so với tháng trước';
        const direction = change.delta > 0 ? 'Tăng' : 'Giảm';
        const percent = change.percent === null ? 'tháng trước chưa có doanh thu' : Math.abs(change.percent).toLocaleString('vi-VN', {maximumFractionDigits:1}) + '%';
        return direction + ' ' + money(Math.abs(change.delta)) + ' · ' + percent;
    }
};
