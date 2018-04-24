lazy val `nfiraos-assembly` = project
  .settings(
    libraryDependencies ++= Dependencies.NfiraosAssembly
  )

lazy val `nfiraos-hcd` = project
  .settings(
    libraryDependencies ++= Dependencies.NfiraosHcd
  )

lazy val `nfiraos-deploy` = project
  .dependsOn(
    `nfiraos-assembly`,
    `nfiraos-hcd`
  )
  .enablePlugins(JavaAppPackaging, CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.NfiraosDeploy
  )
