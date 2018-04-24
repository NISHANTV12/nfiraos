package org.tmt.nfiraos.nfiraosdeploy

import csw.framework.deploy.containercmd.ContainerCmd

object NfiraosContainerCmdApp extends App {

  ContainerCmd.start("nfiraos-container-cmd-app", args)

}
