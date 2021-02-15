package com.base2services.hotwire.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestSseElementType;

import org.jboss.logging.Logger;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.smallrye.mutiny.Multi;

@Path("/stream")
public class StreamingResource {

  private static final Logger LOG = Logger.getLogger(StreamingResource.class);

  private static final AtomicLong counter = new AtomicLong(0);

  @CheckedTemplate
  public static class Templates {
      public static native TemplateInstance stream();
  }

  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestSseElementType("text/vnd.turbo-stream.html")
  @Path("/counter")
  public Multi<String> counterStream(@QueryParam("name") String name) {
    long count = counter.getAndIncrement();
    LOG.info("counter:" + count);
    return Multi
      .createFrom()
      .ticks()
      .every(Duration.ofSeconds(1))
      .onItem()
      .transform(n-> {
        var event = Templates.stream().data("count", count).data("name", name).render();
        LOG.info(event);
        return event;
      })
      .transform()
      .byTakingFirstItems(count);
  }

}