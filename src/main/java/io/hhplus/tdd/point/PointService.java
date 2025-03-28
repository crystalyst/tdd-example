package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    private void validateUserId(long userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid User Id: " + userId);
        }
        UserPoint targetUserPoint = userPointTable.selectById(userId);

        if (targetUserPoint == null) {
            throw new IllegalArgumentException("User Not Found: " + userId);
        }
    }

    public UserPoint getUserPoint(long userId) {
        validateUserId(userId);
        return userPointTable.selectById(userId);
    }

    public List<PointHistory> getUserPointHistory(long userId) {
        validateUserId(userId);
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint chargePoints(long userId, long chargeAmount) {
        if (chargeAmount <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive.");
        }
        validateUserId(userId);

        long currentTime = System.currentTimeMillis();
        UserPoint currentPoint = userPointTable.selectById(userId);
        long newBalance = currentPoint.point() + chargeAmount;

        pointHistoryTable.insert(userId, chargeAmount, TransactionType.CHARGE, currentTime);
        return userPointTable.insertOrUpdate(userId, newBalance);
    }

    public UserPoint usePoints(long userId, long useAmount) {
        if (useAmount <= 0) {
            throw new IllegalArgumentException("Usage amount must be positive.");
        }
        validateUserId(userId);

        long currentTime = System.currentTimeMillis();
        UserPoint currentPoint = userPointTable.selectById(userId);

        if (currentPoint.point() < useAmount) {
            throw new IllegalStateException("Insufficient points for transaction.");
        }

        long newBalance = currentPoint.point() - useAmount;

        pointHistoryTable.insert(userId, -useAmount, TransactionType.USE, currentTime);
        return userPointTable.insertOrUpdate(userId, newBalance);
    }
}
