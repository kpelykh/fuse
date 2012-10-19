/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.fusesource.fabric.webui.patching

import javax.ws.rs._

@Path("/upgrades/versions")
class VersionUpgradeResource extends BaseUpgradeResource {

  @GET
  override def get = not_found

  @GET
  @Path("{id}")
  def possible_upgrades(@PathParam("id") id: String) = {
    val version = get_version(id)
    patch_service.getPossibleUpgrades(version)
  }

  @POST
  @Path("{id}")
  def apply_upgrades(@PathParam("id") id: String, dto: ApplyUpgradesDTO) = {
    val version = get_version(id)
    patch_service.applyUpgrades(version, dto.upgrades)
  }


}