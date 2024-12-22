package com.leikooo.yupicturebackend;

/**
 * @author <a href="https://github.com/lieeew">leikooo</a>
 * @date 2024/12/19
 * @description
 */
public class ConditionTest {

    public static void main(String[] args) {
        boolean a = false;
        boolean b = methodWithSideEffect(); // 即使 a 为 false，b 也会被计算
        boolean result = a || b; // result 为 false
        System.out.println(result);
    }

    public static boolean methodWithSideEffect() {
        System.out.println("Method with side effect is called.");
        return true;
    }

}
