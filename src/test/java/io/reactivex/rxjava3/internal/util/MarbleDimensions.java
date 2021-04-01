/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.util;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.*;

import javax.imageio.ImageIO;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Parses the main sources, locates the {@code <img>} tags, downloads
 * the referenced image and checks if the scaled dimensions are correct.
 */
public final class MarbleDimensions {

    /** Helper program. */
    private MarbleDimensions() {
        throw new IllegalStateException("No instances!");
    }

    public static void main(String[] args) throws Throwable {
        Pattern p = Pattern.compile("\\s*\\*\\s*\\<img\\s+width\\=('|\")(\\d+)('|\")\\s+height\\=('|\")(\\d+)('|\")\\s+src\\=('|\")(.+?)('|\").*");

        Map<String, Integer[]> dimensions = new HashMap<>();

        for (Class<?> clazz : CLASSES) {
            String simpleName = clazz.getSimpleName();
            System.out.println(simpleName);
            System.out.println("----");
            String packageName = clazz.getPackage().getName();

            File f = TestHelper.findSource(clazz.getSimpleName(), packageName);
            if (f == null) {
                System.err.println("Unable to locate " + clazz);
                continue;
            }

            List<String> lines = Files.readAllLines(f.toPath());

            for (int i = 0; i < lines.size(); i++) {
                Matcher m = p.matcher(lines.get(i));
                if (m.matches()) {
                    int width = Integer.parseInt(m.group(2));
                    int height = Integer.parseInt(m.group(5));
                    String url = m.group(8);

                    Integer[] imageDim = dimensions.get(url);
                    if (imageDim == null) {
                        Thread.sleep(SLEEP_PER_IMAGE_MILLIS);

                        try {
                            BufferedImage bimg = ImageIO.read(new URL(url));

                            if (bimg == null) {
                                throw new IOException("not found");
                            }
                            imageDim = new Integer[] { 0, 0 };
                            imageDim[0] = bimg.getWidth();
                            imageDim[1] = bimg.getHeight();

                            dimensions.put(url, imageDim);
                        } catch (IOException ex) {
                            System.err.printf("%s => %s%n", url, ex);
                            System.err.printf(" at %s.%s.method(%s.java:%d)%n", packageName, simpleName, simpleName, i + 1);
                        }
                    }

                    if (imageDim != null) {
                        int expectedHeight = (int)Math.round(1.0 * width / imageDim[0] * imageDim[1]);

                        if (expectedHeight != height) {
                            System.out.printf("    %d => %d%n", height, expectedHeight);
                            System.out.printf(" at %s.%s.method(%s.java:%d)%n", packageName, simpleName, simpleName, i + 1);
                        }
                    }
                    // System.out.printf("%d: %d x %d => %s%n", i + 1, width, height, url);
                }
            }
        }
    }

    static final int SLEEP_PER_IMAGE_MILLIS = 25;

    static final Class<?>[] CLASSES = {
            Flowable.class, Observable.class, Maybe.class, Single.class, Completable.class, ParallelFlowable.class
    };
}
