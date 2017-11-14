package org.mdz.dzp.workflow.neo.engine.jackson;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.mdz.dzp.workflow.neo.engine.model.Message;

public class MessageModule extends SimpleModule {

  public MessageModule() {
    super(PackageVersion.VERSION);

    setMixInAnnotation(Message.class, MessageMixin.class);
  }

}
