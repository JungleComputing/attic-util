package ibis.impl.messagePassing;

import ibis.util.TypedProperties;

interface ElectionProtocol {
    static final byte ELECTION = 99;

    static final boolean NEED_ELECTION = TypedProperties.booleanProperty("ibis.mp.election", true);
}
