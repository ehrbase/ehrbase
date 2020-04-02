/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.validation.constraints.wrappers;

import org.ehrbase.validation.constraints.ConstraintOccurrences;
import org.ehrbase.validation.constraints.util.ZonedDateTimeUtil;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import org.openehr.schemas.v1.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by christian on 7/20/2016.
 * 26.6.19:
 *         migrated from Joda-Time to java.time (see https://blog.joda.org/2014/11/converting-from-joda-time-to-javatime.html)
 *         migrated to nedap Archie
 *
 */
@SuppressWarnings("unchecked")
public class IntervalComparator {

    //lower exclusive, upper exclusive
    private static void isWithinLxUx(Comparable value, Comparable lower, Comparable upper) throws IllegalArgumentException {
        if (value.compareTo(lower) > 0 && value.compareTo(upper) < 0) return;
        throw new IllegalArgumentException("value is not within interval, expected:" + lower + " < " + value + " < " + upper);
    }

    //lower inclusive, upper exclusive
    private static void isWithinLiUx(Comparable value, Comparable lower, Comparable upper) throws IllegalArgumentException {
        if (value.compareTo(lower) >= 0 && value.compareTo(upper) < 0) return;
        throw new IllegalArgumentException("value is not within interval, expected:" + lower + " <= " + value + " < " + upper);
    }

    //lower exclusive, upper inclusive
    private static void isWithinLxUi(Comparable value, Comparable lower, Comparable upper) throws IllegalArgumentException {
        if (value.compareTo(lower) > 0 && value.compareTo(upper) <= 0) return;
        throw new IllegalArgumentException("value is not within interval, expected:" + lower + " < " + value + " <= " + upper);
    }

    //lower inclusive, upper inclusive
    private static void isWithinLiUi(Comparable value, Comparable lower, Comparable upper) throws IllegalArgumentException {
        if (value.compareTo(lower) >= 0 && value.compareTo(upper) <= 0) return;
        throw new IllegalArgumentException("value is not within interval, expected:" + lower + " <= " + value + " <= " + upper);
    }

    private static void compareWithinInterval(Comparable value, Interval interval, Comparable lower, Comparable upper) throws IllegalArgumentException {
        boolean isLowerIncluded = (interval.isSetLowerIncluded() ? interval.getLowerIncluded() : false);
        boolean isUpperIncluded = (interval.isSetUpperIncluded() ? interval.getUpperIncluded() : false);

        if (isLowerIncluded && isUpperIncluded)
            isWithinLiUi(value, lower, upper);
        else if (isLowerIncluded && !isUpperIncluded)
            isWithinLiUx(value, lower, upper);
        else if (!isLowerIncluded && isUpperIncluded)
            isWithinLxUi(value, lower, upper);
        else if (!isLowerIncluded && !isUpperIncluded)
            isWithinLxUx(value, lower, upper);
    }

//    private static void compareWithinInterval(Comparable value, ConstraintOccurrences occurrences, Comparable lower, Comparable upper) throws IllegalArgumentException {
//        boolean isLowerIncluded = (occurrences.getLowerIncluded() ? occurrences.getLowerIncluded() : false);
//        boolean isUpperIncluded = (occurrences.getLowerIncluded() ? occurrences.getUpperIncluded() : false);
//
//        if (isLowerIncluded && isUpperIncluded)
//            isWithinLiUi(value, lower, upper);
//        else if (isLowerIncluded && !isUpperIncluded)
//            isWithinLiUx(value, lower, upper);
//        else if (!isLowerIncluded && isUpperIncluded)
//            isWithinLxUi(value, lower, upper);
//        else if (!isLowerIncluded && !isUpperIncluded)
//            isWithinLxUx(value, lower, upper);
//    }


    public static void isWithinBoundaries(Float real, IntervalOfReal intervalOfReal) throws IllegalArgumentException {
        Float lower = (intervalOfReal.isSetLower() ? intervalOfReal.getLower() : Float.MIN_VALUE);
        Float upper = (intervalOfReal.isSetUpper() ? intervalOfReal.getUpper() : Float.MAX_VALUE);

        compareWithinInterval(real, intervalOfReal, lower, upper);
    }

    public static void isWithinBoundaries(Integer integer, IntervalOfInteger intervalOfInteger) throws IllegalArgumentException {
        Integer lower = (intervalOfInteger.isSetLower() ? intervalOfInteger.getLower() : Integer.MIN_VALUE);
        Integer upper = (intervalOfInteger.isSetUpper() ? intervalOfInteger.getUpper() : Integer.MAX_VALUE);

        compareWithinInterval(integer, intervalOfInteger, lower, upper);
    }

    public static void isWithinBoundaries(Integer integer, ConstraintOccurrences occurrences) throws IllegalArgumentException {
        Integer lower = occurrences.getLower();
        Integer upper = occurrences.getUpper();

        compareWithinInterval(integer, makeOptInterval(occurrences.asInterval()), lower, upper);
    }

    static void isWithinPrecision(Integer integer, IntervalOfInteger intervalOfInteger) throws IllegalArgumentException {
        if (intervalOfInteger == null)
            return;
        Integer lower = (intervalOfInteger.isSetLower() ? intervalOfInteger.getLower() : Integer.MIN_VALUE);
        Integer upper = (intervalOfInteger.isSetUpper() ? intervalOfInteger.getUpper() : Integer.MAX_VALUE);

        try {
            compareWithinInterval(integer, intervalOfInteger, lower, upper);
        } catch (Exception e) {
            throw new IllegalArgumentException("Precision:" + e.getMessage());
        }
    }

    public static void isWithinBoundaries(String rawDate, IntervalOfDate intervalOfDate) throws IllegalArgumentException {

        ZonedDateTime valueDate = ZonedDateTime.parse(rawDate);

        isWithinBoundaries(valueDate, intervalOfDate);
    }

    public static void isWithinBoundaries(DvDate date, IntervalOfDate intervalOfDate) throws IllegalArgumentException {

        ZonedDateTime valueDate = ZonedDateTime.from(date.getValue());

        isWithinBoundaries(valueDate, intervalOfDate);
    }

    public static void isWithinBoundaries(DvDateTime dateTime, IntervalOfDateTime intervalOfDateTime) throws IllegalArgumentException {

        ZonedDateTime valueDate = ZonedDateTime.from(dateTime.getValue());

        isWithinBoundaries(valueDate, intervalOfDateTime);
    }

    public static void isWithinBoundaries(DvTime dateTime, IntervalOfTime intervalOfTime) throws IllegalArgumentException {

        ZonedDateTime valueDate = ZonedDateTime.from(dateTime.getValue());

        isWithinBoundaries(valueDate, intervalOfTime);
    }

    public static void isWithinBoundaries(ZonedDateTime valueDate, IntervalOfDate intervalOfDate) throws IllegalArgumentException {

        String lower = (intervalOfDate.isSetLower() ? intervalOfDate.getLower() : null);
        String upper = (intervalOfDate.isSetUpper() ? intervalOfDate.getUpper() : null);

        ZonedDateTime lowerDate, upperDate;
        //Date massage...
        if (lower != null)
            lowerDate = ZonedDateTime.parse(lower);
        else
            lowerDate =  new ZonedDateTimeUtil().min();

        if (upper != null)
            upperDate = ZonedDateTime.parse(upper);
        else
            upperDate = new ZonedDateTimeUtil().max();

        compareWithinInterval(valueDate, intervalOfDate, lowerDate, upperDate);
    }

    public static void isWithinBoundaries(String rawDateTime, IntervalOfDateTime intervalOfDateTime) throws IllegalArgumentException {

        ZonedDateTime valueDateTime = ZonedDateTime.parse(rawDateTime);

        isWithinBoundaries(valueDateTime, intervalOfDateTime);

    }

    public static void isWithinBoundaries(ZonedDateTime valueDateTime, IntervalOfDateTime intervalOfDateTime) throws IllegalArgumentException {

        String lower = (intervalOfDateTime.isSetLower() ? intervalOfDateTime.getLower() : null);
        String upper = (intervalOfDateTime.isSetUpper() ? intervalOfDateTime.getUpper() : null);

        ZonedDateTime lowerDateTime, upperDateTime;
        //Date massage...
        if (lower != null)
            lowerDateTime = ZonedDateTime.parse(lower);
        else
            lowerDateTime = new ZonedDateTimeUtil().min();

        if (upper != null)
            upperDateTime = ZonedDateTime.parse(upper);
        else
            upperDateTime = new ZonedDateTimeUtil().max();

        compareWithinInterval(valueDateTime, intervalOfDateTime, lowerDateTime, upperDateTime);
    }

    public static void isWithinBoundaries(String rawTime, IntervalOfTime intervalOfTime) throws IllegalArgumentException {
        ZonedDateTime valueTime = ZonedDateTime.parse(rawTime);
        isWithinBoundaries(valueTime, intervalOfTime);
    }

    public static void isWithinBoundaries(ZonedDateTime valueTime, IntervalOfTime intervalOfTime) throws IllegalArgumentException {

        String lower = (intervalOfTime.isSetLower() ? intervalOfTime.getLower() : null);
        String upper = (intervalOfTime.isSetUpper() ? intervalOfTime.getUpper() : null);

        ZonedDateTime lowerTime, upperTime;
        //Date massage...
        if (lower != null)
            lowerTime = ZonedDateTime.parse(lower);
        else
            lowerTime = new ZonedDateTimeUtil().min();


        if (upper != null)
            upperTime = ZonedDateTime.parse(upper);
        else
            upperTime = new ZonedDateTimeUtil().min();

        compareWithinInterval(valueTime, intervalOfTime, lowerTime, upperTime);
    }

    public static void isWithinBoundaries(String rawDuration, IntervalOfDuration intervalOfDuration) throws IllegalArgumentException {

        Duration valueDuration = Duration.parse(rawDuration);
        isWithinBoundaries(valueDuration, intervalOfDuration);

    }

    public static void isWithinBoundaries(Duration valueDuration, IntervalOfDuration intervalOfDuration) throws IllegalArgumentException {

        String lower = (intervalOfDuration.isSetLower() ? intervalOfDuration.getLower() : null);
        String upper = (intervalOfDuration.isSetUpper() ? intervalOfDuration.getUpper() : null);

        Duration lowerDuration, upperDuration;
        //Date massage...
        if (lower != null)
            lowerDuration = Duration.parse(lower);
        else
            lowerDuration = Duration.ZERO;

        if (upper != null)
            upperDuration = Duration.parse(upper);
        else
            upperDuration = Duration.of(Long.MAX_VALUE, ChronoUnit.FOREVER);

        compareWithinInterval(valueDuration, intervalOfDuration, lowerDuration, upperDuration);
    }

    public static boolean isOptional(IntervalOfInteger intervalOfInteger) {
        if (intervalOfInteger.isSetLower() && intervalOfInteger.getLower() == 1 && intervalOfInteger.isSetUpper() && intervalOfInteger.getUpper() == 1)
            return false;

        try {
            isWithinBoundaries(0, intervalOfInteger);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * convert between an RM interval and an OPT (XML) interval of Integer
     * @param integerInterval
     * @return
     */
    private static IntervalOfInteger makeOptInterval(com.nedap.archie.base.Interval<Integer> integerInterval) {
        IntervalOfInteger intervalOfInteger = IntervalOfInteger.Factory.newInstance();
        intervalOfInteger.setLower(integerInterval.getLower());
        intervalOfInteger.setUpper(integerInterval.getUpper());
        intervalOfInteger.setLowerIncluded(integerInterval.isLowerIncluded());
        intervalOfInteger.setUpperIncluded(integerInterval.isUpperIncluded());
        return intervalOfInteger;
    }

    public static String toString(IntervalOfInteger intervalOfInteger) {
        StringBuilder stringBuffer = new StringBuilder();

        stringBuffer.append("[");
        if (intervalOfInteger.isSetLower())
            stringBuffer.append(intervalOfInteger.getLower());
        else
            stringBuffer.append("*");
        stringBuffer.append("..");
        if (intervalOfInteger.isSetUpper())
            stringBuffer.append(intervalOfInteger.getUpper());
        else
            stringBuffer.append("*");
        stringBuffer.append("]");

        return stringBuffer.toString();
    }

    public static String toString(com.nedap.archie.base.Interval<Integer> intervalOfInteger) {
        StringBuilder stringBuffer = new StringBuilder();

        stringBuffer.append("[");
        if (intervalOfInteger.getLower() != null && intervalOfInteger.getLower() != Integer.MIN_VALUE)
            stringBuffer.append(intervalOfInteger.getLower());
        else
            stringBuffer.append("*");
        stringBuffer.append("..");
        if (intervalOfInteger.getUpper() != null && intervalOfInteger.getUpper() != Integer.MAX_VALUE)
            stringBuffer.append(intervalOfInteger.getUpper());
        else
            stringBuffer.append("*");
        stringBuffer.append("]");

        return stringBuffer.toString();
    }

}
