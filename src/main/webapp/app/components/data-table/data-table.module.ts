import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { DataTableComponent } from './data-table.component';
import { DataDableSearchControlsComponent } from 'app/components/data-table/data-dable-search-controls.component';
import { ServerSidePaginatedDatatableComponent } from 'app/components/data-table/server-side-paginated-datatable.component';

@NgModule({
    imports: [ArtemisSharedModule, NgxDatatableModule],
    declarations: [DataTableComponent, DataDableSearchControlsComponent, ServerSidePaginatedDatatableComponent],
    exports: [DataTableComponent, ServerSidePaginatedDatatableComponent],
})
export class ArtemisDataTableModule {}
