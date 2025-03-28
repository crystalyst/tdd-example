package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    // 1. GET Points
    @Test
    void getUserPoint_ValidUserId_ReturnsUserPoint() {
        long userId = 1L;
        UserPoint mockPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        UserPoint result = pointService.getUserPoint(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals(100L, result.point());
        verify(userPointTable, times(2)).selectById(userId);
    }

    @Test
    void getUserPoint_InvalidUserId_ThrowsException() {
        long userId = -1L;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.getUserPoint(userId));

        assertEquals("Invalid User Id: -1", exception.getMessage());
    }

    @Test
    void getUserPoint_NonExistentUser_ThrowsException() {
        long userId = 999L;
        when(userPointTable.selectById(userId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.getUserPoint(userId));

        assertEquals("User Not Found: 999", exception.getMessage());
    }

    // 2. Charge Points
    @Test
    void chargePoints_ValidUserIdAndAmount_ReturnsUpdatedUserPoint() {
        long userId = 1L;
        long chargeAmount = 50L;
        UserPoint mockPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);
        when(userPointTable.insertOrUpdate(userId, 150L)).thenReturn(new UserPoint(userId, 150L, System.currentTimeMillis()));

        UserPoint result = pointService.chargePoints(userId, chargeAmount);

        assertNotNull(result);
        assertEquals(150L, result.point());
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void chargePoints_InvalidAmount_ThrowsException() {
        long userId = 1L;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> pointService.chargePoints(userId, -50L));

        assertEquals("Charge amount must be positive.", exception.getMessage());
    }

    // 3. Use Points
    @Test
    void usePoints_ValidUserIdAndAmount_ReturnsUpdatedUserPoint() {
        long userId = 1L;
        long useAmount = 30L;
        UserPoint mockPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);
        when(userPointTable.insertOrUpdate(userId, 70L)).thenReturn(new UserPoint(userId, 70L, System.currentTimeMillis()));

        UserPoint result = pointService.usePoints(userId, useAmount);

        assertNotNull(result);
        assertEquals(70L, result.point());
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(-useAmount), eq(TransactionType.USE), anyLong());
    }

    @Test
    void usePoints_InsufficientBalance_ThrowsException() {
        long userId = 1L;
        long useAmount = 150L;
        UserPoint mockPoint = new UserPoint(userId, 100L, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockPoint);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pointService.usePoints(userId, useAmount));

        assertEquals("Insufficient points for transaction.", exception.getMessage());
    }

    // 4. Point History Tests
    @Test
    void getUserPointHistory_ValidUserId_ReturnsHistoryList() {
        long userId = 1L;
        List<PointHistory> history = List.of(
                new PointHistory(1L, userId, 100L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, -50L, TransactionType.USE, System.currentTimeMillis())
        );
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(history);

        List<PointHistory> result = pointService.getUserPointHistory(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
