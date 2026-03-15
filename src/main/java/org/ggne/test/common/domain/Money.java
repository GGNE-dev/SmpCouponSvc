package org.ggne.test.common.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Money {

    private int amount;       // DB의 컬럼이 되는 필드

    public Money(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다. 입력값: " + amount);
        }
        this.amount = amount;
    }

    // 덧셈
    public Money add(Money other) {
        if (other == null) {
            return this;
        }
        return new Money(this.amount + other.amount);
    }

    // 뺄셈 (결과가 음수면 0 반환)
    public Money subtract(Money other) {
        if (other == null) {
            return this;
        }
        return new Money(Math.max(0, this.amount - other.amount));
    }

    // 정률 할인 계산
    public Money multiplyByRate(int rate) {
        if (rate < 0 || rate > 100) {
            throw new IllegalArgumentException("할인율은 0~100 사이여야 합니다. 입력값: " + rate);
        }
        return new Money((int) (this.amount * (rate / 100.0)));
    }

    @Override
    public String toString() {
        return amount + "원";
    }
}