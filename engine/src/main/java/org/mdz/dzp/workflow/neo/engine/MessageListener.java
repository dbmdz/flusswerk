package org.mdz.dzp.workflow.neo.engine;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.mdz.dzp.workflow.neo.engine.model.Message;

public interface MessageListener {

  void receive(Message message, Long deliveryTag, Channel channel) throws IOException;

}
