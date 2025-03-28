package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    private final long validUserId = 1L;
    private final long invalidUserId = -1L;
    private final long chargeAmount = 100L;
    private final long useAmount = 50L;

    private UserPoint userPoint;

    @BeforeEach
    void setUp() {
        userPoint = new UserPoint(validUserId, 200L, System.currentTimeMillis());
    }

    @Test
    void getUserPoint_InvalidUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pointService.getUserPoint(invalidUserId));
    }

    @Test
    void chargePoints_NegativeAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pointService.chargePoints(validUserId, -10L));
    }

    @Test
    void usePoints_NegativeAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pointService.usePoints(validUserId, -10L));
    }

    @Test
    void getUserPoint_ValidUserId_ReturnsUserPoint() {
        when(userPointTable.selectById(validUserId)).thenReturn(userPoint);

        UserPoint result = pointService.getUserPoint(validUserId);

        assertNotNull(result);
        assertEquals(validUserId, result.id());
        assertEquals(200L, result.point());
        verify(userPointTable, times(2)).selectById(validUserId);
    }

    @Test
    void chargePoints_ValidUserIdAndAmount_ReturnsUpdatedUserPoint() {
        when(userPointTable.selectById(validUserId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(validUserId, 300L)).thenReturn(new UserPoint(validUserId, 300L, System.currentTimeMillis()));

        UserPoint result = pointService.chargePoints(validUserId, chargeAmount);

        assertNotNull(result);
        assertEquals(300L, result.point());
        verify(pointHistoryTable, times(1)).insert(eq(validUserId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    void usePoints_ValidUserIdAndAmount_ReturnsUpdatedUserPoint() {
        when(userPointTable.selectById(validUserId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(validUserId, 150L)).thenReturn(new UserPoint(validUserId, 150L, System.currentTimeMillis()));

        UserPoint result = pointService.usePoints(validUserId, useAmount);

        assertNotNull(result);
        assertEquals(150L, result.point());
        verify(pointHistoryTable, times(1)).insert(eq(validUserId), eq(-useAmount), eq(TransactionType.USE), anyLong());
    }
    @Test
    void usePoints_InsufficientBalance_ThrowsException() {
        when(userPointTable.selectById(validUserId)).thenReturn(userPoint);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> pointService.usePoints(validUserId, 300L));
        assertEquals("Insufficient points for transaction.", exception.getMessage());
    }

    @Test
    void getUserPointHistory_InvalidUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> pointService.getUserPointHistory(invalidUserId));
    }
}
