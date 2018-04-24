package org.tmt.nfiraos.nfiraosassembly.messages

import csw.services.command.scaladsl.CommandService

sealed trait WorkerCommand
case class CommandForHcd(hcd: CommandService) extends WorkerCommand
