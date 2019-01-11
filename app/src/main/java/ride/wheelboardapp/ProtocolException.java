package ride.wheelboardapp;

import proto.Protocol;

class ProtocolException extends  Exception {
    public final Protocol.ReplyId replyId;

    public ProtocolException(Protocol.ReplyId replyId) {
        this.replyId = replyId;
    }
}
