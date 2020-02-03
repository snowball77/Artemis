import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { DataTableComponent } from './data-table.component';
import { DataDableSearchControlsComponent } from 'app/components/data-table/data-dable-search-controls.component';

@NgModule({
    imports: [ArtemisSharedModule, NgxDatatableModule],
    declarations: [DataTableComponent, DataDableSearchControlsComponent],
    exports: [DataTableComponent],
})
export class ArtemisDataTableModule {}
