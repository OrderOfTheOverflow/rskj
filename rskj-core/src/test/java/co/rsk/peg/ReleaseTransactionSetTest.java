/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.peg;

import co.rsk.bitcoinj.core.*;
import co.rsk.config.TestSystemProperties;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseTransactionSetTest {
    private Set<ReleaseTransactionSet.Entry> setEntries;
    private ReleaseTransactionSet set;
    private final TestSystemProperties config = new TestSystemProperties();

    @BeforeEach
    void createSet() {
        setEntries = new HashSet<>(Arrays.asList(
            new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 32L),
            new ReleaseTransactionSet.Entry(createTransaction(5, Coin.COIN), 100L),
            new ReleaseTransactionSet.Entry(createTransaction(4, Coin.FIFTY_COINS), 7L),
            new ReleaseTransactionSet.Entry(createTransaction(3, Coin.MILLICOIN), 10L),
            new ReleaseTransactionSet.Entry(createTransaction(8, Coin.CENT.times(5)), 5L)
        ));
        set = new ReleaseTransactionSet(setEntries);
    }

    @Test
    void entryEquals() {
        ReleaseTransactionSet.Entry e1 = new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 15L);
        ReleaseTransactionSet.Entry e2 = new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 15L);
        ReleaseTransactionSet.Entry e3 = new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(149)), 14L);
        ReleaseTransactionSet.Entry e4 = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(230)), 15L);
        ReleaseTransactionSet.Entry e5 = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(230)), 15L, PegTestUtils.createHash3(0));
        ReleaseTransactionSet.Entry e6 = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(230)), 15L, PegTestUtils.createHash3(0));
        ReleaseTransactionSet.Entry e7 = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(230)), 15L, null);
        ReleaseTransactionSet.Entry e8 = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(230)), 15L, null);

        Assertions.assertEquals(e1, e2);
        Assertions.assertNotEquals(e1, e3);
        Assertions.assertNotEquals(e1, e4);
        Assertions.assertEquals(e5, e6);
        Assertions.assertNotEquals(e5, e7);
        Assertions.assertEquals(e7, e8);
    }

    @Test
    void entryGetters() {
        ReleaseTransactionSet.Entry entry = new ReleaseTransactionSet.Entry(createTransaction(5, Coin.valueOf(100)), 7L);

        Assertions.assertEquals(createTransaction(5, Coin.valueOf(100)), entry.getTransaction());
        Assertions.assertEquals(7L, entry.getRskBlockNumber().longValue());
    }

    @Test
    void entryComparators() {
        ReleaseTransactionSet.Entry e1 = new ReleaseTransactionSet.Entry(mockTxSerialize("aa"), 7L);
        ReleaseTransactionSet.Entry e2 = new ReleaseTransactionSet.Entry(mockTxSerialize("aa"), 7L);
        ReleaseTransactionSet.Entry e3 = new ReleaseTransactionSet.Entry(mockTxSerialize("aa"), 8L);
        ReleaseTransactionSet.Entry e4 = new ReleaseTransactionSet.Entry(mockTxSerialize("bb"), 7L);
        ReleaseTransactionSet.Entry e5 = new ReleaseTransactionSet.Entry(mockTxSerialize("99"), 7L);

        Assertions.assertEquals(0, ReleaseTransactionSet.Entry.BTC_TX_COMPARATOR.compare(e1, e2));
        Assertions.assertEquals(0, ReleaseTransactionSet.Entry.BTC_TX_COMPARATOR.compare(e1, e3));
        Assertions.assertTrue(ReleaseTransactionSet.Entry.BTC_TX_COMPARATOR.compare(e1, e4) < 0);
        Assertions.assertTrue(ReleaseTransactionSet.Entry.BTC_TX_COMPARATOR.compare(e1, e5) > 0);
    }

    @Test
    void entriesCopy() {
        Assertions.assertNotSame(setEntries, set.getEntries());
        Assertions.assertEquals(setEntries, set.getEntries());

        Set<ReleaseTransactionSet.Entry> entryWithoutHash = new HashSet<>(Collections.singletonList(
                new ReleaseTransactionSet.Entry(new BtcTransaction(config.getNetworkConstants().getBridgeConstants().getBtcParams()), 1L)
        ));

        Set<ReleaseTransactionSet.Entry> entryWithHash = new HashSet<>(Collections.singletonList(
                new ReleaseTransactionSet.Entry(new BtcTransaction(config.getNetworkConstants().getBridgeConstants().getBtcParams()), 1L, PegTestUtils.createHash3(0))
        ));

        ReleaseTransactionSet transactionSetWithoutHash = new ReleaseTransactionSet(entryWithoutHash);
        ReleaseTransactionSet transactionSetWithHash = new ReleaseTransactionSet(entryWithHash);

        Set<ReleaseTransactionSet.Entry> resultCallWithoutHash = transactionSetWithoutHash.getEntriesWithoutHash();
        Assertions.assertEquals(resultCallWithoutHash, entryWithoutHash);

        Set<ReleaseTransactionSet.Entry> resultCallWithHash = transactionSetWithoutHash.getEntriesWithHash();
        Assertions.assertEquals(0, resultCallWithHash.size());

        Set<ReleaseTransactionSet.Entry> resultCallWithoutHash2 = transactionSetWithHash.getEntriesWithoutHash();
        Assertions.assertEquals(0, resultCallWithoutHash2.size());

        Set<ReleaseTransactionSet.Entry> resultCallWithHash2 = transactionSetWithHash.getEntriesWithHash();
        Assertions.assertEquals(resultCallWithHash2, entryWithHash);
    }

    @Test
    void add_nonExisting() {
        Assertions.assertFalse(set.getEntries().contains(new ReleaseTransactionSet.Entry(createTransaction(123, Coin.COIN.multiply(3)), 34L)));
        set.add(createTransaction(123, Coin.COIN.multiply(3)), 34L);
        Assertions.assertTrue(set.getEntries().contains(new ReleaseTransactionSet.Entry(createTransaction(123, Coin.COIN.multiply(3)), 34L)));
    }

    @Test
    void add_existing() {
        Assertions.assertTrue(set.getEntries().contains(new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 32L)));
        Assertions.assertEquals(1, set.getEntries().stream().filter(e -> e.getTransaction().equals(createTransaction(2, Coin.valueOf(150)))).count());
        set.add(createTransaction(2, Coin.valueOf(150)), 23L);
        Assertions.assertTrue(set.getEntries().contains(new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 32L)));
        Assertions.assertFalse(set.getEntries().contains(new ReleaseTransactionSet.Entry(createTransaction(2, Coin.valueOf(150)), 23L)));
        Assertions.assertEquals(1, set.getEntries().stream().filter(e -> e.getTransaction().equals(createTransaction(2, Coin.valueOf(150)))).count());
    }

    @Test
    void sliceWithConfirmations_withLimit_none() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(9L, 5, Optional.of(1));
        Assertions.assertEquals(0, result.size());
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    @Test
    void sliceWithConfirmations_withLimit_singleMatch() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(10L, 5, Optional.of(2));
        Assertions.assertEquals(1, result.size());
        setEntries.remove(new ReleaseTransactionSet.Entry(createTransaction(8, Coin.CENT.times(5)), 5L));
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    @Test
    void sliceWithConfirmations_withLimit_multipleMatch() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(15L, 5, Optional.of(2));
        Assertions.assertEquals(2, result.size());
        Iterator<ReleaseTransactionSet.Entry> resultIterator = result.iterator();
        while (resultIterator.hasNext()) {
            ReleaseTransactionSet.Entry itemTx = resultIterator.next();
            ReleaseTransactionSet.Entry item = setEntries.stream().filter(e -> e.getTransaction().equals(itemTx.getTransaction())).findFirst().get();
            Assertions.assertTrue(15L - item.getRskBlockNumber() >= 5);
            setEntries.remove(item);
        }
        Assertions.assertEquals(3, set.getEntries().size());
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    @Test
    void sliceWithConfirmations_noLimit_none() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(9L, 5, Optional.empty());
        Assertions.assertEquals(0, result.size());
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    @Test
    void sliceWithConfirmations_noLimit_singleMatch() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(10L, 5, Optional.empty());
        Assertions.assertEquals(1, result.size());
        setEntries.remove(new ReleaseTransactionSet.Entry(createTransaction(8, Coin.CENT.times(5)), 5L));
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    @Test
    void sliceWithConfirmations_noLimit_multipleMatch() {
        Set<ReleaseTransactionSet.Entry> result = set.sliceWithConfirmations(15L, 5, Optional.empty());
        Assertions.assertEquals(3, result.size());
        Iterator<ReleaseTransactionSet.Entry> resultIterator = result.iterator();
        while (resultIterator.hasNext()) {
            ReleaseTransactionSet.Entry itemTx = resultIterator.next();
            ReleaseTransactionSet.Entry item = setEntries.stream().filter(e -> e.getTransaction().equals(itemTx.getTransaction())).findFirst().get();
            Assertions.assertTrue(15L - item.getRskBlockNumber() >= 5);
            setEntries.remove(item);
        }
        Assertions.assertEquals(2, set.getEntries().size());
        Assertions.assertEquals(setEntries, set.getEntries());
    }

    private BtcTransaction createTransaction(int toPk, Coin value) {
        NetworkParameters params = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
        BtcTransaction input = new BtcTransaction(params);
        input.addOutput(Coin.FIFTY_COINS, BtcECKey.fromPrivate(BigInteger.valueOf(123456)).toAddress(params));

        Address to = BtcECKey.fromPrivate(BigInteger.valueOf(toPk)).toAddress(params);

        BtcTransaction result = new BtcTransaction(params);
        result.addInput(input.getOutput(0));
        result.getInput(0).disconnect();
        result.addOutput(value, to);
        return result;
    }

    private BtcTransaction mockTxSerialize(String serializationHex) {
        BtcTransaction result = mock(BtcTransaction.class);
        when(result.bitcoinSerialize()).thenReturn(Hex.decode(serializationHex));
        return result;
    }
}
