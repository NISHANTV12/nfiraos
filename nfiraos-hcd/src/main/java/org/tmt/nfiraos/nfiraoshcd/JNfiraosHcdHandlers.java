package org.tmt.nfiraos.nfiraoshcd;

import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Adapter;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.scaladsl.CurrentStatePublisher;
import csw.messages.commands.*;
import csw.messages.framework.ComponentInfo;
import csw.messages.location.*;
import csw.messages.location.Connection.HttpConnection;
import csw.messages.scaladsl.TopLevelActorMessage;
import csw.services.command.scaladsl.CommandResponseManager;
import csw.services.config.api.javadsl.IConfigClientService;
import csw.services.config.api.models.ConfigData;
import csw.services.config.client.javadsl.JConfigClientFactory;
import csw.services.config.client.scaladsl.ConfigClientFactory;
import csw.services.location.javadsl.ILocationService;
import csw.services.logging.javadsl.ILogger;
import csw.services.logging.javadsl.JLoggerFactory;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static csw.services.location.javadsl.JComponentType.Service;

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to NfiraosHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
public class JNfiraosHcdHandlers extends JComponentHandlers {

    private ILogger log;
    private CommandResponseManager commandResponseManager;
    private CurrentStatePublisher currentStatePublisher;
    private ActorContext<TopLevelActorMessage> actorContext;
    private ILocationService locationService;
    private ComponentInfo componentInfo;
    private ConfigData hcdConfig;
    private IConfigClientService configClient;

    JNfiraosHcdHandlers(
          ActorContext<TopLevelActorMessage> ctx,
          ComponentInfo componentInfo,
          CommandResponseManager commandResponseManager,
          CurrentStatePublisher currentStatePublisher,
          ILocationService locationService,
          JLoggerFactory loggerFactory
    ) {
        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory);
        this.currentStatePublisher = currentStatePublisher;
        this.log = loggerFactory.getLogger(getClass());
        this.commandResponseManager = commandResponseManager;
        this.actorContext = ctx;
        this.locationService = locationService;
        this.componentInfo = componentInfo;
    }

    //initialize
    @Override
    public CompletableFuture<Void> jInitialize() {
        return CompletableFuture.runAsync(() -> {

            //track configuration service
            HttpConnection configServer = new HttpConnection(new ComponentId("ConfigServer", Service));
            trackConnection(configServer);

            configClient = JConfigClientFactory.clientApi(Adapter.toUntyped(actorContext.getSystem()), locationService);
            CompletableFuture<ConfigData> configDataF = configClient.getActive(Paths.get("tromboneAssemblyContext.conf")).thenApply((maybeConfigData) -> maybeConfigData.<RuntimeException>orElseThrow(RuntimeException::new));
            configDataF.thenAccept(config -> hcdConfig = config);
        });
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return CompletableFuture.runAsync(() -> log.info("[HCD] Shutting down..."));
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
        log.info(() -> "TrackingEvent received: " + trackingEvent.connection().name());

        if(trackingEvent instanceof LocationUpdated) {
            Location location = ((LocationUpdated) trackingEvent).location();
            log.info(() -> "location updated for " + location.connection().name());
        }
        else if (trackingEvent instanceof LocationRemoved) {
            Connection connection = trackingEvent.connection();
            log.info(() -> "location removed for " + connection.name());
        }

    }

    @Override
    public CommandResponse validateCommand(ControlCommand controlCommand) {
        log.info("[HCD] Validating received command: " + controlCommand.commandName());

        if(controlCommand.commandName().name().equals("sleep")) {
            return new CommandResponse.Accepted(controlCommand.runId());
        } else {
            return new CommandResponse.Invalid(controlCommand.runId(), new CommandIssue.UnsupportedCommandIssue("Command " + controlCommand.commandName().name() + " not supported"));
        }
    }

    @Override
    public void onSubmit(ControlCommand controlCommand) {
        log.info(() -> "[HCD] Received command: " + controlCommand.commandName());

        if(controlCommand instanceof Setup) {
            log.info(() -> "[HCD] Received setup");
            // process setup command
        }
        else if (controlCommand instanceof Observe) {
            log.info(() -> "[HCD] Received observe");
            // process observe command
        }
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }
}
