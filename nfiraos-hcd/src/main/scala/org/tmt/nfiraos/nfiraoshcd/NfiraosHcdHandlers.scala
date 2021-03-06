package org.tmt.nfiraos.nfiraoshcd

import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.commands._
import csw.messages.framework.ComponentInfo
import csw.messages.location.ComponentType.Service
import csw.messages.location.Connection.HttpConnection
import csw.messages.location._
import csw.messages.params.generics.{Key, KeyType, Parameter}
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import org.tmt.nfiraos.nfiraoshcd.internal.Worker
import org.tmt.nfiraos.nfiraoshcd.messages.Sleep

import scala.async.Async.async
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to NfiraosHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
 */
class NfiraosHcdHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: LocationService,
    loggerFactory: LoggerFactory
) extends ComponentHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, loggerFactory) {

  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  implicit val mat: Materializer            = ActorMaterializer()(ctx.system.toUntyped)

  private val log                           = loggerFactory.getLogger
  private val workerActor                   = new Worker(ctx, loggerFactory, commandResponseManager)
  private var hcdConfig: String             = _

  //initialize
  override def initialize(): Future[Unit] = Future {
    log.info("---------------> Initializing ...")
    //track configuration service
    //trackConnection(HttpConnection(ComponentId("ConfigServer", Service)))

    //    import java.nio.file.Paths
    //    import csw.services.config.client.scaladsl.ConfigClientFactory
    //    val configClientService = ConfigClientFactory.clientApi(ctx.system.toUntyped, locationService)
    //
    //    val maybeConfigData = await(configClientService.getActive(Paths.get("hcdConfig.conf")))
    //
    //    maybeConfigData match {
    //      case Some(data) ⇒ hcdConfig = await(data.toStringF)
    //      case _          ⇒
    //    }
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.info(s"---------------> TrackingEvent received: ${trackingEvent.connection.name}")

    trackingEvent match {
      case LocationUpdated(location) ⇒ // Do something on location update
      case LocationRemoved(connection) ⇒ // Do something on location removed
    }
  }

  override def onShutdown(): Future[Unit] = Future {
    log.info("---------------> Shutting down...")
  }

  //validate
  override def validateCommand(controlCommand: ControlCommand): CommandResponse = {
    log.info(s"---------------> Validating received command: ${controlCommand.commandName}")
    controlCommand.commandName.name match {
      case "sleep" => CommandResponse.Accepted(controlCommand.runId)
      case x       => CommandResponse.Invalid(controlCommand.runId, CommandIssue.UnsupportedCommandIssue(s"Command $x. not supported."))
    }
  }

  //onSubmit
  override def onSubmit(controlCommand: ControlCommand): Unit = {
    log.info(s"---------------> Received command: ${controlCommand.commandName}")

    controlCommand match {
      case setupCommand: Setup     => onSetup(setupCommand)
      case observeCommand: Observe => // implement (or not)
    }
  }

  private def onSetup(setup: Setup): Unit = {
    val sleepTimeKey: Key[Long] = KeyType.LongKey.make("SleepTime")

    // get param from the Parameter Set in the Setup
    val sleepTimeParam: Parameter[Long] = setup(sleepTimeKey)

    // values of parameters are arrays. Get the first one (the only one in our case) using `head` method available as a convenience method on `Parameter`.
    val sleepTimeInMillis: Long = sleepTimeParam.head

    log.info(s"---------------> command payload: ${sleepTimeParam.keyName} = $sleepTimeInMillis")

    workerActor.actor ! Sleep(setup.runId, sleepTimeInMillis)
  }
  //onSubmit

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

}
