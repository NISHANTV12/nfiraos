package org.tmt.nfiraos.nfiraosdeploy

import csw.framework.deploy.hostconfig.HostConfig

object NfiraosHostConfigApp extends App {

  HostConfig.start("nfiraos-host-config-app", args)

}
