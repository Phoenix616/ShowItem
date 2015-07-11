# ShowItem
Show your items to other players via chat.

#### Commands
```
showitem:
  aliases: [show,si]
  description: Will use the default radius from the config if no player or radius is given!
  usage: /<command> [<player>|-r(adius) <radius>|-reload]
  permission: showitem.command
```
#### Permissions
```
showitem.command:
   description: Gives permission to the some command
   default: op
showitem.command.player:
   description: Gives permission to show an item to a specific player wherever he is
   default: op
showitem.command.radius:
   description: Gives permission to show an item in radius you give
   default: op
showitem.command.reload:
   description: Gives permission to use the reload command
   default: op
```       
#### Copyright
The main plugin is licensed under the GPLv3 license as stated in the copyright notice below:
```
Copyright (C) 2015 Phoenix616.All rights reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation,  version 3.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
```
##### Extra licenses
This plugin uses [libraries](https://github.com/Minebench/ShowItem/tree/master/src/de/themoep/utils) licensed under the GPLv3 compatible [MPL v2.0](https://www.mozilla.org/MPL/2.0/) and [MIT](http://opensource.org/licenses/MIT) licenses!
