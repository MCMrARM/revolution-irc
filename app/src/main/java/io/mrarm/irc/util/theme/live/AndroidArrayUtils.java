/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mrarm.irc.util.theme.live;

import java.lang.reflect.Array;

class AndroidArrayUtils {

    static <T> T[] append(T[] array, int currentSize, T element) {
        if (currentSize + 1 > array.length) {
            @SuppressWarnings("unchecked")
            T[] newArray = (T[]) Array.newInstance((Class<T>) array.getClass().getComponentType(),
                    growSize(currentSize));
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    static int[] append(int[] array, int currentSize, int element) {
        if (currentSize + 1 > array.length) {
            int[] newArray = new int[growSize(currentSize)];
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    static float[] append(float[] array, int currentSize, float element) {
        if (currentSize + 1 > array.length) {
            float[] newArray = new float[growSize(currentSize)];
            System.arraycopy(array, 0, newArray, 0, currentSize);
            array = newArray;
        }
        array[currentSize] = element;
        return array;
    }

    static int growSize(int currentSize) {
        return currentSize <= 4 ? 8 : currentSize * 2;
    }

}
